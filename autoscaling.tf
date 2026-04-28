resource "aws_appautoscaling_target" "api" {
  max_capacity       = 4
  min_capacity       = 1
  resource_id        = "service/${aws_ecs_cluster.ageoverflow.name}/${aws_ecs_service.ageoverflow.name}"
  scalable_dimension = "ecs:service:DesiredCount"
  service_namespace  = "ecs"

  depends_on = [aws_ecs_service.ageoverflow]
}

resource "aws_appautoscaling_policy" "api_cpu" {
  name               = "ageoverflow-api-cpu"
  policy_type        = "TargetTrackingScaling"
  resource_id        = aws_appautoscaling_target.api.resource_id
  scalable_dimension = aws_appautoscaling_target.api.scalable_dimension
  service_namespace  = aws_appautoscaling_target.api.service_namespace

  target_tracking_scaling_policy_configuration {
    target_value       = 70.0
    scale_in_cooldown  = 60
    scale_out_cooldown = 60

    predefined_metric_specification {
      predefined_metric_type = "ECSServiceAverageCPUUtilization"
    }
  }
}

resource "aws_appautoscaling_target" "worker" {
  max_capacity       = 20
  min_capacity       = 1
  resource_id        = "service/${aws_ecs_cluster.ageoverflow.name}/${aws_ecs_service.ageoverflow_worker.name}"
  scalable_dimension = "ecs:service:DesiredCount"
  service_namespace  = "ecs"

  depends_on = [aws_ecs_service.ageoverflow_worker]
}

resource "aws_cloudwatch_metric_alarm" "urgent_queue_high" {
  alarm_name          = "ageoverflow-urgent-queue-high"
  comparison_operator = "GreaterThanThreshold"
  evaluation_periods  = 1
  metric_name         = "ApproximateNumberOfMessagesVisible"
  namespace           = "AWS/SQS"
  period              = 60
  statistic           = "Sum"
  threshold           = 0
  alarm_actions       = [aws_appautoscaling_policy.worker_scale_out.arn]
  dimensions = {
    QueueName = aws_sqs_queue.urgent.name
  }
}

resource "aws_cloudwatch_metric_alarm" "standard_queue_high" {
  alarm_name          = "ageoverflow-standard-queue-high"
  comparison_operator = "GreaterThanThreshold"
  evaluation_periods  = 1
  metric_name         = "ApproximateNumberOfMessagesVisible"
  namespace           = "AWS/SQS"
  period              = 60
  statistic           = "Sum"
  threshold           = 0
  alarm_actions       = [aws_appautoscaling_policy.worker_scale_out.arn]
  dimensions = {
    QueueName = aws_sqs_queue.standard.name
  }
}

resource "aws_cloudwatch_metric_alarm" "urgent_queue_low" {
  alarm_name          = "ageoverflow-urgent-queue-low"
  comparison_operator = "LessThanOrEqualToThreshold"
  evaluation_periods  = 3
  metric_name         = "ApproximateNumberOfMessagesVisible"
  namespace           = "AWS/SQS"
  period              = 60
  statistic           = "Sum"
  threshold           = 0
  alarm_actions       = [aws_appautoscaling_policy.worker_scale_in.arn]
  dimensions = {
    QueueName = aws_sqs_queue.urgent.name
  }
}

resource "aws_cloudwatch_metric_alarm" "standard_queue_low" {
  alarm_name          = "ageoverflow-standard-queue-low"
  comparison_operator = "LessThanOrEqualToThreshold"
  evaluation_periods  = 3
  metric_name         = "ApproximateNumberOfMessagesVisible"
  namespace           = "AWS/SQS"
  period              = 60
  statistic           = "Sum"
  threshold           = 0
  alarm_actions       = [aws_appautoscaling_policy.worker_scale_in.arn]
  dimensions = {
    QueueName = aws_sqs_queue.standard.name
  }
}

resource "aws_appautoscaling_policy" "worker_scale_out" {
  name               = "ageoverflow-worker-scale-out"
  policy_type        = "StepScaling"
  resource_id        = aws_appautoscaling_target.worker.resource_id
  scalable_dimension = aws_appautoscaling_target.worker.scalable_dimension
  service_namespace  = aws_appautoscaling_target.worker.service_namespace

  step_scaling_policy_configuration {
    adjustment_type         = "ChangeInCapacity"
    cooldown                = 30
    metric_aggregation_type = "Maximum"

    step_adjustment {
      metric_interval_lower_bound = 0
      metric_interval_upper_bound = 50
      scaling_adjustment          = 3
    }

    step_adjustment {
      metric_interval_lower_bound = 50
      scaling_adjustment          = 5
    }
  }
}

resource "aws_appautoscaling_policy" "worker_scale_in" {
  name               = "ageoverflow-worker-scale-in"
  policy_type        = "StepScaling"
  resource_id        = aws_appautoscaling_target.worker.resource_id
  scalable_dimension = aws_appautoscaling_target.worker.scalable_dimension
  service_namespace  = aws_appautoscaling_target.worker.service_namespace

  step_scaling_policy_configuration {
    adjustment_type         = "ChangeInCapacity"
    cooldown                = 120
    metric_aggregation_type = "Maximum"

    step_adjustment {
      metric_interval_upper_bound = 0
      scaling_adjustment          = -1
    }
  }
}
