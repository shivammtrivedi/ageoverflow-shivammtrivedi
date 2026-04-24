resource "aws_sqs_queue" "urgent" {
  name                       = "ageoverflow-urgent"
  visibility_timeout_seconds = 300
}

resource "aws_sqs_queue" "standard" {
  name                       = "ageoverflow-standard"
  visibility_timeout_seconds = 300
}

output "urgent_queue_url" {
  value = aws_sqs_queue.urgent.url
}

output "standard_queue_url" {
  value = aws_sqs_queue.standard.url
}
