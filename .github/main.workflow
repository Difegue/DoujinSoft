workflow "Build Docker image" {
  resolves = [
    "Push Image",
  ]
  on = "push"
}

action "GitHub Action for Maven" {
  uses = "LucaFeger/action-maven-cli@1.1.0"
  args = "clean package"
}

action "Build Image" {
  uses = "actions/docker/cli@8cdf801b322af5f369e00d85e9cf3a7122f49108"
  needs = ["GitHub Action for Maven"]
  args = "build -t difegue/doujinsoft -f ./Docker/Dockerfile ."
}

action "Login to Docker Hub" {
  uses = "actions/docker/login@8cdf801b322af5f369e00d85e9cf3a7122f49108"
  needs = ["Build Image"]
  secrets = ["DOCKER_USERNAME", "DOCKER_PASSWORD"]
}

action "Push Image" {
  uses = "actions/docker/cli@8cdf801b322af5f369e00d85e9cf3a7122f49108"
  needs = ["Login to Docker Hub"]
  args = "push difegue/doujinsoft"
}
