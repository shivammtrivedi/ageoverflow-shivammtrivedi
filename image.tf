resource "docker_image" "ageoverflow" {
  name = "${aws_ecr_repository.ageoverflow.repository_url}:latest"
  build {
    context  = "./gcas-api"
    platform = "linux/amd64"
  }
}

resource "docker_registry_image" "ageoverflow" {
  name = docker_image.ageoverflow.name
}