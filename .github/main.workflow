workflow "Build Docker image" {
  resolves = ["GitHub Action for Maven"]
  on = "commit_comment"
}

workflow "Build Docker image" {
  resolves = ["GitHub Action for Maven"]
  on = "push"
}

action "GitHub Action for Maven" {
  uses = "LucaFeger/action-maven-cli@1.1.0"
  args = "clean package"
}
