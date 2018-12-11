/*
 * Copyright (c) 2010-2018 Poterion. All rights reserved.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS
 * IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO,
 * THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR
 * PURPOSE ARE DISCLAIMED.  IN NO EVENT SHALL THE COPYRIGHT OWNER OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 * PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

/* REQUIRED SCRIPT APPROVAL
 *
 * - method java.util.Properties setProperty java.lang.String java.lang.String
 * - method org.jenkinsci.plugins.workflow.steps.FlowInterruptedException getCauses
 * - method org.jenkinsci.plugins.workflow.support.steps.input.Rejection getUser
 * - new hudson.model.ChoiceParameterDefinition java.lang.String java.lang.String[] java.lang.String
 * - new java.io.File java.lang.String
 * - new java.util.Properties
 * - staticMethod org.codehaus.groovy.runtime.DefaultGroovyMethods newWriter java.io.File
 */

/* REQUIRED CREDENTTIALS
 *
 * - poterion-git               [SSH Key]   (global)
 * - google-play-api-key        [File]      (local)
 * - keystore-file              [File]      (local)
 * - keystore-password          [Secret]    (local)
 * - key-password               [Secret]    (local)
 */

def setup() {
    if (BRANCH_NAME == 'master' || BRANCH_NAME ==~ /release\/.*/) {
        //return VersionNumber(
        //        projectStartDate: '2014-10-03',
        //        skipFailedBuilds: true,
        //        versionNumberString: '${BUILDS_ALL_TIME}',
        //        versionPrefix: '')
        env.VERSION_CODE = sh(returnStdout: true, script: 'git tag | grep -E \'^v[0-9]+\\.[0-9]+\' | sort -r | head -n 1 | sed -E \'s/^v[0-9]+\\.([0-9]+)/\\1/\'').toInteger() + 2
    } else {
        env.VERSION_CODE = 1
    }
    if (BRANCH_NAME == 'master' || BRANCH_NAME ==~ /release\/.*/) {
        //return VersionNumber(
        //        projectStartDate: '2014-10-03',
        //        skipFailedBuilds: true,
        //        versionNumberString: '${YEARS_SINCE_PROJECT_START}.${BUILD_MONTH}.${BUILDS_ALL_TIME}',
        //        versionPrefix: '')
        env.VERSION_NAME = readFile('version') + "." + (VERSION_CODE.toInteger() - 1)
    } else {
        env.VERSION_NAME = "Snapshot"
    }

    if (BRANCH_NAME == 'master' || BRANCH_NAME ==~ /release\/.*/) {
        currentBuild.displayName = "#${BUILD_NUMBER}: ${VERSION_NAME} (${VERSION_CODE})"
    } else if (BRANCH_NAME ==~ /\w+\/.*/) {
        currentBuild.displayName = BRANCH_NAME.replaceAll(/\w+\/(.*)/) { matches ->
            "${matches[1]}.${BUILD_NUMBER}"
        }
    } else {
        version = "${BRANCH_NAME}.${BUILD_NUMBER}"
    }

    echo "Building version: ${VERSION_NAME} (${VERSION_CODE})"
    sh 'chmod +x gradlew'
}

pipeline {
    agent any
    tools {
        jdk 'JDK_8'
    }
    options {
        timeout(time: 4, unit: 'HOURS')
        disableConcurrentBuilds()
        buildDiscarder(logRotator(numToKeepStr: '20', artifactNumToKeepStr: '20'))
    }
    environment {
        PACKAGE = 'com.poterion.logbook'
        ANDROID_SDK_MIN = 16
        JAVA7_HOME = "/usr/lib/jvm/java-7-oracle"
        JAVA8_HOME = "/usr/lib/jvm/java-8-oracle"
        ANDROID_HOME = "/opt/android-sdk-linux"
        ANDROID_SDK_ROOT = "/opt/android-sdk-linux"
        ANDROID_ZIPALIGN = "/opt/android-sdk-linux/build-tools/26.0.2/zipalign"
        PATH = "${ANDROID_SDK_ROOT}/emulator:${ANDROID_SDK_ROOT}/tools:${ANDROID_SDK_ROOT}/platform-tools:${PATH}"
    }
    stages {
        stage('Checkout') {
            //when { expression { BRANCH_NAME == "skip" } }
            steps {
                script {
                    if (BRANCH_NAME ==~ /release\/.*/) { // BRANCH_NAME == "master" ||
                        cleanWs()
                    }

                    sh 'java -version'
                    sh 'javac -version'
                    sh 'echo $PATH'
                    sh 'echo $JAVA_HOME'

                    checkout([
                            $class                           : 'GitSCM',
                            branches                         : [
                                    [name: "*/${BRANCH_NAME}"]
                                    //[name: '*/master'],
                                    //[name: ':refs/heads/release\/.*']
                            ],
                            doGenerateSubmoduleConfigurations: false,
                            extensions                       : [
                                    //[$class: 'WipeWorkspace'],
                                    //[$class: 'CleanBeforeCheckout'],
                                    [$class      : 'CloneOption',
                                     depth       : 0,
                                     honorRefspec: true,
                                     noTags      : false,
                                     reference   : '',
                                     shallow     : true
                                    ],
                                    [$class             : 'SubmoduleOption',
                                     disableSubmodules  : false,
                                     parentCredentials  : true,
                                     recursiveSubmodules: true,
                                     reference          : '',
                                     trackingSubmodules : false]
                            ],
                            submoduleCfg                     : [],
                            userRemoteConfigs                : [
                                    [
                                            credentialsId: 'poterion-git',
                                            url          : 'ssh://git@bitbucket.intra:7999/monitor/raspi-w2812-android.git'
                                    ]
                            ]
                    ])
                    setup()
                }
            }
        }
        stage('Build') {
            //when { expression { BRANCH_NAME == "skip" } }
            when { expression { BRANCH_NAME == 'master' || BRANCH_NAME ==~ /(feature|bugfix)\/.*/ || BRANCH_NAME ==~ /PR-\d+/ } }
            steps {
                setup()
                lock(resource: 'ws2812-build-android', inversePrecedence: true) {
                    sh './gradlew --daemon --stacktrace clean assembleDebug'
                    milestone(10)
                }
            }
        }
        stage('Unit Tests') {
            //when { expression { BRANCH_NAME == "skip" } }
            when { expression { BRANCH_NAME == 'master' || BRANCH_NAME ==~ /(feature|bugfix)\/.*/ || BRANCH_NAME ==~ /PR-\d+/ } }
            steps {
                setup()
                lock(resource: 'ws2812-test-android', inversePrecedence: true) {
                    sh './gradlew --daemon --stacktrace testDebugUnitTest'
                    milestone(30)
                }
            }
            post {
                always {
                    archiveArtifacts artifacts: 'app/build/jacoco/*.exec', allowEmptyArchive: true
                }
            }
        }
        stage('Instrumentation Tests') {
            //when { expression { BRANCH_NAME == "skip" } }
            when { expression { BRANCH_NAME == 'master' || BRANCH_NAME ==~ /(feature|bugfix)\/.*/ } }
            steps {
                setup()
                lock(resource: 'android-emulator', inversePrecedence: true) {
                    sh './gradlew --daemon --stacktrace deviceAndroidTest'
                    milestone(40)
                }
            }
            post {
                always {
                    archiveArtifacts artifacts: // 'app/build/spoon-output/*/coverage/merged-coverage.ec,' +
                            'app/build/outputs/code-coverage/results/**/*.ec,' +
                            'app/build/outputs/code-coverage/results/**/*.log,' +
                            'app/build/screenshots/**/*',
                            allowEmptyArchive: true
                    //publishHTML([allowMissing         : true,
                    //             alwaysLinkToLastBuild: true,
                    //             keepAll              : true,
                    //             reportDir            : 'app/build/spoon-output/debug',
                    //             reportFiles          : 'index.html',
                    //             reportName           : 'Spoon Report',
                    //             reportTitles         : ''])
                }
            }
        }
        stage('Build Release') {
            //when { expression { BRANCH_NAME == "skip" } }
            when { expression { BRANCH_NAME == 'master' || BRANCH_NAME ==~ /(feature|bugfix|release)\/.*/ || BRANCH_NAME ==~ /PR-\d+/ } }
            steps {
                setup()
                lock(resource: 'ws2812-build-release-android', inversePrecedence: true) {
                    //sh 'export SDK_VERSION_MIN=16'
                    withCredentials([file(credentialsId: 'keystore-file', variable: 'KEYSTORE_FILE'),
                                     string(credentialsId: 'keystore-password', variable: 'KEYSTORE_PASSWORD'),
                                     string(credentialsId: 'key-password', variable: 'KEY_PASSWORD')]) {
                        sh './gradlew --daemon --stacktrace assembleRelease'
                    }
                    milestone(70)
                }
            }
            post {
                always {
                    //sh './gradlew --daemon --stacktrace uninstallAll'
                    archiveArtifacts artifacts: 'app/build/reports/lint-results-*.xml,app/build/outputs/apk/release/com.poterion.raspi.w2812.android-*.apk', allowEmptyArchive: true
                    publishHTML([allowMissing         : true,
                                 alwaysLinkToLastBuild: true,
                                 keepAll              : true,
                                 reportDir            : 'app/build/reports',
                                 reportFiles          : 'lint-result-release.html',
                                 reportName           : 'Lint Report',
                                 reportTitles         : 'Lint Report'])
                    androidLint defaultEncoding: '',
                            pattern: 'app/build/reports/lint-results-*.xml',
                            healthy: '300',
                            unHealthy: '2500',
                            failedNewAll: '2000',
                            failedNewHigh: '5',
                            failedNewNormal: '2000',
                            failedNewLow: '3000',
                            failedTotalAll: '2000',
                            failedTotalHigh: '5',
                            failedTotalNormal: '2000',
                            failedTotalLow: '3000',
                            unstableNewAll: '200',
                            unstableNewHigh: '1',
                            unstableNewNormal: '100',
                            unstableNewLow: '150',
                            unstableTotalAll: '1000',
                            unstableTotalHigh: '1',
                            unstableTotalNormal: '500',
                            unstableTotalLow: '1000',
                            useStableBuildAsReference: true
                }
            }
        }
        stage('Publish') {
            when { expression { BRANCH_NAME == 'master' || BRANCH_NAME ==~ /release\/.*/ } }
            steps {
                script {
                    setup()
                    def release = ['stage': 'none']
                    try {
                        def parameters = []

                        if (BRANCH_NAME ==~ /release\/.*/) {
                            parameters.addAll([
                                    new ChoiceParameterDefinition('stage', ['internal', 'alpha', 'beta', 'rollout', 'production'] as String[], 'Stage'),
                                    string(defaultValue: '20', description: 'User Fraction', name: 'userFraction', trim: true),
                                    booleanParam(defaultValue: true, description: 'Untrack old releases', name: 'untrackOld')])
                        } else {
                            parameters.add(new ChoiceParameterDefinition('stage', ['alpha'] as String[], 'Stage'))
                        }
                        parameters.addAll([])
                        release = input id: 'should-release',
                                message: 'Release',
                                ok: 'Yes',
                                parameters: parameters,
                                //submitter: 'project-ws2812',
                                submitterParameter: 'approvedBy'
                        doPublish = true
                    } catch (err) { // input false
                        doPublish = false
                        def user = err.getCauses()[0].getUser()
                        echo "Aborted by: [${user}]"
                        if (BRANCH_NAME ==~ /release\/.*/) {
                            currentBuild.result = 'ABORTED'
                        }
                    }

                    echo "Should Release: ${doPublish}"

                    if (doPublish) {
                        lock(resource: 'ws2812-release-android', inversePrecedence: true) {
                            env.PUBLISH_STAGE = release['stage'] ?: 'alpha'
                            env.PUBLISH_UNTRACK_OLD = release['untrackOld'] ? "true" : "false"
                            env.PUBLISH_USER_FRACTION = ((release['userFraction'] ?: 10) as Integer) / 100
                            echo "Releasing to ${PUBLISH_STAGE} stage for ${PUBLISH_USER_FRACTION} users untracking: ${PUBLISH_UNTRACK_OLD}..."

                            //step([
                            //        $class             : 'SignApksBuilder',
                            //        apksToSign         : '**/*-unsigned.apk',
                            //        archiveUnsignedApks: true,
                            //        keyAlias           : "${PACKAGE}",
                            //        keyStoreId         : 'signing-certificate',
                            //        zipalignPath       : "${ANDROID_ZIPALIGN}"
                            //])

                            sh("git config user.name 'Jenkins'")
                            sh("git config user.email 'jenkins@poterion.com'")
                            sh "git tag v${VERSION_NAME} || (git tag -d v${VERSION_NAME} && git tag v${VERSION_NAME})"
                            sshagent(credentials: ['poterion-git']) {
                                //sh("git tag -a v${VERSION_NAME} -m 'Release ${VERSION_NAME}'")
                                sh('git push origin --tags')
                            }

                            withCredentials([file(credentialsId: 'google-play-api-key', variable: 'PUBLISH_KEY_FILE'),
                                             file(credentialsId: 'keystore-file', variable: 'KEYSTORE_FILE'),
                                             string(credentialsId: 'keystore-password', variable: 'KEYSTORE_PASSWORD'),
                                             string(credentialsId: 'key-password', variable: 'KEY_PASSWORD')]) {
                                sh './gradlew --daemon --stacktrace publishRelease'
                            }
                            //androidApkUpload googleCredentialsId: 'google-play',
                            //        apkFilesPattern: '**/*.apk',
                            //        trackName: 'alpha',
                            //        recentChangeList: [
                            //                [language: 'en-GB', text: "Please test the changes from Jenkins build ${env.BUILD_NUMBER}."],
                            //                [language: 'de-DE', text: "Bitte die Änderungen vom Jenkins Build ${env.BUILD_NUMBER} testen."]
                            //        ]
                        }
                    }
                    milestone(80)
                }
            }
            post {
                always {
                    archiveArtifacts artifacts: 'app/build/outputs/apk/release/com.poterion.logbook-*.apk', allowEmptyArchive: true
                }
            }
        }
    }
}
