terraform {
  required_providers {
    aws = {
      source  = "hashicorp/aws"
      version = "~> 6.0"
    }
    docker = {
      source  = "kreuzwerker/docker"
      version = "3.6.2"
    }
  }
}

provider "aws" {
  region                   = "us-east-1"
  shared_credentials_files = ["./credentials"]
}

locals {
  image             = "${aws_ecr_repository.ageoverflow.repository_url}:latest"
  database_username = "administrator"
  database_password = "verySecretPassword" # This is bad!
}

resource "aws_db_instance" "ageoverflow_database" {
  allocated_storage      = 20
  max_allocated_storage  = 1000
  engine                 = "postgres"
  engine_version         = "16"
  instance_class         = "db.t3.micro"
  db_name                = "ageoverflowdb"
  username               = local.database_username
  password               = local.database_password
  parameter_group_name   = "default.postgres16"
  skip_final_snapshot    = true
  vpc_security_group_ids = [aws_security_group.ageoverflow_database.id]
  publicly_accessible    = true

  tags = {
    Name = "ageoverflow_database"
  }
}


resource "aws_security_group" "ageoverflow_database" {
  name        = "ageoverflow_database"
  description = "Allow inbound Postgresql traffic"

  ingress {
    from_port   = 5432
    to_port     = 5432
    protocol    = "tcp"
    cidr_blocks = ["0.0.0.0/0"]
  }

  egress {
    from_port        = 0
    to_port          = 0
    protocol         = "-1"
    cidr_blocks      = ["0.0.0.0/0"]
    ipv6_cidr_blocks = ["::/0"]
  }

  tags = {
    Name = "ageoverflow_database"
  }
}

data "aws_iam_role" "lab" {
  name = "LabRole"
}

data "aws_vpc" "default" {
  default = true
}

data "aws_subnets" "private" {
  filter {
    name   = "vpc-id"
    values = [data.aws_vpc.default.id]
  }
}

resource "aws_ecs_cluster" "ageoverflow" {
  name = "ageoverflow"
}

resource "aws_ecs_task_definition" "ageoverflow" {
  family                   = "ageoverflow"
  network_mode             = "awsvpc"
  requires_compatibilities = ["FARGATE"]
  cpu                      = 1024
  memory                   = 2048
  execution_role_arn       = data.aws_iam_role.lab.arn
  task_role_arn            = data.aws_iam_role.lab.arn

  container_definitions = <<DEFINITION
 [
   {
    "image": "${local.image}",
    "cpu": 1024,
    "memory": 2048,
    "name": "ageoverflow",
    "networkMode": "awsvpc",
    "portMappings": [
      {
       "containerPort": 8080,
       "hostPort": 8080
      }
    ],
    "environment": [
      {
       "name": "SPRING_DATASOURCE_URL",
       "value": "jdbc:postgresql://${aws_db_instance.ageoverflow_database.address}:${aws_db_instance.ageoverflow_database.port}/${aws_db_instance.ageoverflow_database.db_name}"
      },
      {
       "name": "SPRING_DATASOURCE_DRIVER_CLASS_NAME",
       "value": "org.postgresql.Driver"
      },
      {
       "name": "SPRING_DATASOURCE_USERNAME",
       "value": "${local.database_username}"
      },
      {
       "name": "SPRING_DATASOURCE_PASSWORD",
       "value": "${local.database_password}"
      },
      {
       "name": "SPRING_JPA_HIBERNATE_DDL_AUTO",
       "value": "update"
      },
      {
       "name": "GCAS_MODE",
       "value": "api"
      },
      {
       "name": "SQS_URGENT_QUEUE_URL",
       "value": "${aws_sqs_queue.urgent.url}"
      },
      {
       "name": "SQS_STANDARD_QUEUE_URL",
       "value": "${aws_sqs_queue.standard.url}"
      }
    ],
    "logConfiguration": {
      "logDriver": "awslogs",
      "options": {
       "awslogs-group": "/ageoverflow",
       "awslogs-region": "us-east-1",
       "awslogs-stream-prefix": "ecs",
       "awslogs-create-group": "true"
      }
    }
   }
 ]
 DEFINITION
}

resource "aws_ecs_task_definition" "ageoverflow_worker" {
  family                   = "ageoverflow-worker"
  network_mode             = "awsvpc"
  requires_compatibilities = ["FARGATE"]
  cpu                      = 1024
  memory                   = 2048
  execution_role_arn       = data.aws_iam_role.lab.arn
  task_role_arn            = data.aws_iam_role.lab.arn

  container_definitions = <<DEFINITION
 [
   {
    "image": "${local.image}",
    "cpu": 1024,
    "memory": 2048,
    "name": "ageoverflow-worker",
    "networkMode": "awsvpc",
    "environment": [
      {
       "name": "SPRING_DATASOURCE_URL",
       "value": "jdbc:postgresql://${aws_db_instance.ageoverflow_database.address}:${aws_db_instance.ageoverflow_database.port}/${aws_db_instance.ageoverflow_database.db_name}"
      },
      {
       "name": "SPRING_DATASOURCE_DRIVER_CLASS_NAME",
       "value": "org.postgresql.Driver"
      },
      {
       "name": "SPRING_DATASOURCE_USERNAME",
       "value": "${local.database_username}"
      },
      {
       "name": "SPRING_DATASOURCE_PASSWORD",
       "value": "${local.database_password}"
      },
      {
       "name": "SPRING_JPA_HIBERNATE_DDL_AUTO",
       "value": "update"
      },
      {
       "name": "GCAS_MODE",
       "value": "worker"
      },
      {
       "name": "SQS_URGENT_QUEUE_URL",
       "value": "${aws_sqs_queue.urgent.url}"
      },
      {
       "name": "SQS_STANDARD_QUEUE_URL",
       "value": "${aws_sqs_queue.standard.url}"
      }
    ],
    "logConfiguration": {
      "logDriver": "awslogs",
      "options": {
       "awslogs-group": "/ageoverflow-worker",
       "awslogs-region": "us-east-1",
       "awslogs-stream-prefix": "ecs",
       "awslogs-create-group": "true"
      }
    }
   }
 ]
 DEFINITION
}

resource "aws_ecs_service" "ageoverflow_worker" {
  name            = "ageoverflow-worker"
  cluster         = aws_ecs_cluster.ageoverflow.id
  task_definition = aws_ecs_task_definition.ageoverflow_worker.arn
  desired_count   = 1
  launch_type     = "FARGATE"

  network_configuration {
    subnets          = data.aws_subnets.private.ids
    security_groups  = [aws_security_group.ageoverflow.id]
    assign_public_ip = true
  }
}

resource "aws_ecs_service" "ageoverflow" {
  name            = "ageoverflow"
  cluster         = aws_ecs_cluster.ageoverflow.id
  task_definition = aws_ecs_task_definition.ageoverflow.arn
  desired_count   = 1
  launch_type     = "FARGATE"

  network_configuration {
    subnets          = data.aws_subnets.private.ids
    security_groups  = [aws_security_group.ageoverflow.id]
    assign_public_ip = true
  }

  load_balancer {
    target_group_arn = aws_lb_target_group.ageoverflow.arn
    container_name   = "ageoverflow"
    container_port   = 8080
  }
}

resource "aws_security_group" "ageoverflow" {
  name        = "ageoverflow"
  description = "AgeOverflow Security Group"

  ingress {
    from_port   = 8080
    to_port     = 8080
    protocol    = "tcp"
    cidr_blocks = ["0.0.0.0/0"]
  }

  ingress {
    from_port   = 22
    to_port     = 22
    protocol    = "tcp"
    cidr_blocks = ["0.0.0.0/0"]
  }

  egress {
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    cidr_blocks = ["0.0.0.0/0"]
  }
}

data "aws_ecr_authorization_token" "ecr_token" {}

provider "docker" {
  registry_auth {
    address  = data.aws_ecr_authorization_token.ecr_token.proxy_endpoint
    username = data.aws_ecr_authorization_token.ecr_token.user_name
    password = data.aws_ecr_authorization_token.ecr_token.password
  }
}

resource "aws_ecr_repository" "ageoverflow" {
  name = "ageoverflow"
}
