def call(Map config) {

pipeline {

    agent any

    tools {
        nodejs config.nodeTool
    }

    options {
        skipDefaultCheckout(true)
    }

    stages {

        stage('Checkout') {
            steps {
                git branch: config.branch,
                    credentialsId: config.gitCredentials,
                    url: config.gitRepo
            }
        }

        stage('Increment Version') {

            steps {
                script {

                    dir(config.appDirectory) {

                        sh '''
                        npm version minor --no-git-tag-version
                        '''

                        def packageJson = readJSON file: 'package.json'
                        def version = packageJson.version

                        env.IMAGE_NAME = "${version}-${BUILD_NUMBER}"
                    }
                }
            }
        }

        stage('Run Tests') {

            steps {

                dir(config.appDirectory) {

                    sh 'npm ci'
                    sh 'npm test'
                }
            }
        }

        stage('Build and Push Docker Image') {

            steps {

                withCredentials([
                    usernamePassword(
                        credentialsId: config.dockerCredentials,
                        usernameVariable: 'USER',
                        passwordVariable: 'PASS'
                    )
                ]) {

                    script {

                        sh """
                        docker build -t ${config.dockerRepo}:${env.IMAGE_NAME} .
                        """

                        sh '''
                        echo $PASS | docker login -u $USER --password-stdin
                        '''

                        sh """
                        docker push ${config.dockerRepo}:${env.IMAGE_NAME}
                        """
                    }
                }
            }
        }

        stage('Commit Version Update') {

            steps {

                withCredentials([
                    usernamePassword(
                        credentialsId: config.gitPushCredentials,
                        usernameVariable: 'USER',
                        passwordVariable: 'TOKEN'
                    )
                ]) {

                    sh """
                    git config user.email "jenkins@example.com"
                    git config user.name "Jenkins"

                    git remote set-url origin https://\$USER:\$TOKEN@github.com/${config.githubRepo}.git

                    git add ${config.appDirectory}/package.json
                    git commit -m "ci: version bump" || true

                    git push origin HEAD:${config.branch}
                    """
                }
            }
        }

    }
}
}
