def call(body) {
    // evaluate the body block, and collect configuration into the object
    def pipelineParams= [:]
    body.resolveStrategy = Closure.DELEGATE_FIRST
    body.delegate = pipelineParams
    body()

    def rtMaven = ''
    def buildInfo = ''

    pipeline {
        agent any

        options {
            buildDiscarder(logRotator(numToKeepStr: '3'))
        }

        environment {
            POM = 'pom.xml'      
        }

        parameters { booleanParam(name: 'SKIP_TESTS', defaultValue: false, description: 'Check if you want to skip tests') }

        stages {
            stage('Checkout git repository') {
	        steps {
                    git branch: pipelineParams.branch, credentialsId: pipelineParams.scmCredentials, url: pipelineParams.scmUrl
                }
            }

            stage('Maven Build') {
                steps {	
                	rtMaven = Artifactory.newMavenBuild()
                    buildInfo = rtMaven.run pom: 'pom.xml', goals: 'clean install'
                }
            }

            stage('Upload') {              
                steps {
                    script {
                        def server = Artifactory.server "jfrog-artifactory"
                        rtMaven.deployer server: server, releaseRepo: 'company-release', snapshotRepo: 'company-snapshot'
                        rtMaven.tool = 'Maven 3.5.2'
                        server.publishBuildInfo buildInfo
                    }
                }
            }


            stage('Clean') {
                steps {
                    cleanWs()
                }
            }
        }
    }
}
