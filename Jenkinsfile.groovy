//def email_prod = 'ilko_sim@mail.bg'
def email_prod = 'ilko_sim@mail.bg'
//def email_qa = 'ilko_sim@mail.bg'
def email_qa = 'ilko_sim@mail.bg'

def cyt_cron(passiveNode, activeNode, Env, App) {
    echo "Setup cytric crontab to passive NODE ${passiveNode}"
    def remote = [:]
    remote.name = "${passiveNode}"
    remote.host = "${passiveNode}"
    remote.allowAnyHosts = true
    withCredentials([sshUserPrivateKey(credentialsId: 'jenkins-ssh-user', keyFileVariable: 'identity', passphraseVariable: 'passphrase', usernameVariable: 'userName')]) {
                remote.user = userName
                remote.identityFile = identity
                remote.passphrase = passphrase
                sshPut remote: remote, from: "configurations/${Env}/${App}/crontab.active", into: '/tmp/'
                sshCommand remote: remote, command: 'chmod 644 /tmp/crontab.active'
                sshCommand remote: remote, command: 'sudo -u cytric crontab /tmp/crontab.active'
                sshCommand remote: remote, command: 'rm -f /tmp/crontab.active'
    }
    echo "Setup cytric crontab to active NODE ${activeNode}"
    remote.name = "${activeNode}"
    remote.host = "${activeNode}"
    remote.allowAnyHosts = true
    withCredentials([sshUserPrivateKey(credentialsId: 'jenkins-ssh-user', keyFileVariable: 'identity', passphraseVariable: 'passphrase', usernameVariable: 'userName')]) {
                remote.user = userName
                remote.identityFile = identity
                remote.passphrase = passphrase
                sshPut remote: remote, from: "configurations/${Env}/${App}/crontab.passive", into: '/tmp/'
                sshCommand remote: remote, command: 'chmod 644 /tmp/crontab.passive'
                sshCommand remote: remote, command: 'sudo -u cytric crontab /tmp/crontab.passive'
                sshCommand remote: remote, command: 'rm -f /tmp/crontab.passive'
    }
}
def cyt_scripts(passiveNode, activeNode, extractDist, Env, App) {
    echo "Setup cytric scripts on passive NODE ${passiveNode}"
    def remote = [:]
    remote.name = "${passiveNode}"
    remote.host = "${passiveNode}"
    remote.allowAnyHosts = true
    withCredentials([sshUserPrivateKey(credentialsId: 'jenkins-ssh-user', keyFileVariable: 'identity', passphraseVariable: 'passphrase', usernameVariable: 'userName')]) {
                remote.user = userName
                remote.identityFile = identity
                remote.passphrase = passphrase
                sshCommand remote: remote, command: 'mkdir -p /tmp/cyt_scripts'
                sshPut remote: remote, from: "scripts/${Env}/${App}/extractdist.sh", into: '/tmp/cyt_scripts/'
                sshPut remote: remote, from: "scripts/${Env}/${App}/bnf.sh", into: '/tmp/cyt_scripts/'
                sshCommand remote: remote, command: 'chmod -R 755 /tmp/cyt_scripts'
                sshCommand remote: remote, command: "sudo -u cytric cp -fp /tmp/cyt_scripts/extractdist.sh ${extractDist}"
                sshCommand remote: remote, command: 'sudo -u cytric cp -fp /tmp/cyt_scripts/bnf.sh /home/ifao/bin/'
                sshCommand remote: remote, command: 'rm -rf /tmp/cyt_scripts'
    }
    echo "Setup cytric scripts on active NODE ${activeNode}"
    remote.name = "${activeNode}"
    remote.host = "${activeNode}"
    remote.allowAnyHosts = true
    withCredentials([sshUserPrivateKey(credentialsId: 'jenkins-ssh-user', keyFileVariable: 'identity', passphraseVariable: 'passphrase', usernameVariable: 'userName')]) {
                remote.user = userName
                remote.identityFile = identity
                remote.passphrase = passphrase
                sshCommand remote: remote, command: 'mkdir -p /tmp/cyt_scripts'
                sshPut remote: remote, from: "scripts/${Env}/${App}/extractdist.sh", into: '/tmp/cyt_scripts/'
                sshPut remote: remote, from: "scripts/${Env}/${App}/bnf.sh", into: '/tmp/cyt_scripts/'
                sshCommand remote: remote, command: 'chmod -R 755 /tmp/cyt_scripts'
                sshCommand remote: remote, command: "sudo -u cytric cp -fp /tmp/cyt_scripts/extractdist.sh ${extractDist}"
                sshCommand remote: remote, command: 'sudo -u cytric cp -fp /tmp/cyt_scripts/bnf.sh /home/ifao/bin/'
                sshCommand remote: remote, command: 'rm -rf /tmp/cyt_scripts'
    }
}
def search_start(Node, search_script) {
    echo "Starting search application on ${Node}"
    def remote = [:]
    remote.name = "${Node}"
    remote.host = "${Node}"
    remote.allowAnyHosts = true
    withCredentials([sshUserPrivateKey(credentialsId: 'jenkins-ssh-user', keyFileVariable: 'identity', passphraseVariable: 'passphrase', usernameVariable: 'userName')]) {
            remote.user = userName
            remote.identityFile = identity
            remote.passphrase = passphrase
            sshCommand remote: remote, command: "sudo -u cytric ${search_script} start"
            sleep(15)
            searchStatusPassive = sshCommand remote: remote, command: "sudo -u cytric ${search_script} status_simple"
            if (searchStatusPassive == 'OK') {
            echo 'The search application is started'
            } else {
            error("Please start search application on ${Node} manually and run the pipeline again.")
            }
    }
}
def search_stop(Node, search_script, script_stop_option) {
    echo "Stopping search application on ${Node}"
    def remote = [:]
    remote.name = "${Node}"
    remote.host = "${Node}"
    remote.allowAnyHosts = true
    withCredentials([sshUserPrivateKey(credentialsId: 'jenkins-ssh-user', keyFileVariable: 'identity', passphraseVariable: 'passphrase', usernameVariable: 'userName')]) {
            remote.user = userName
            remote.identityFile = identity
            remote.passphrase = passphrase
            sshCommand remote: remote, command: "sudo -u cytric ${search_script} ${script_stop_option}"
    }
}
def send_email(body, subject, recipient) {
    emailext (
        body: "${body} \n More info at: ${env.BUILD_URL}",
        subject: "${subject}",
        to: "${recipient}"
    )
}
def build_failed(recipient) {
    emailext (
        body: "${currentBuild.currentResult}: Job ${env.JOB_NAME} build ${env.BUILD_NUMBER}\n More info at: ${env.BUILD_URL}",
        subject: "Jenkins Build ${currentBuild.currentResult}: Job ${env.JOB_NAME}",
        to: "${recipient}"
    )
}
def lsyncd(Node, action) {
    def remote = [:]
    remote.name = "${Node}"
    remote.host = "${Node}"
    remote.allowAnyHosts = true
    withCredentials([sshUserPrivateKey(credentialsId: 'jenkins-ssh-user', keyFileVariable: 'identity', passphraseVariable: 'passphrase', usernameVariable: 'userName')]) {
            remote.user = userName
            remote.identityFile = identity
            remote.passphrase = passphrase
            sshCommand remote: remote, command: "sudo /bin/systemctl ${action} lsyncd"
    }
}
def setup_bnf(Node, action) {
    def remote = [:]
    remote.name = "${Node}"
    remote.host = "${Node}"
    remote.allowAnyHosts = true
    withCredentials([sshUserPrivateKey(credentialsId: 'jenkins-ssh-user', keyFileVariable: 'identity', passphraseVariable: 'passphrase', usernameVariable: 'userName')]) {
            remote.user = userName
            remote.identityFile = identity
            remote.passphrase = passphrase
            sshCommand remote: remote, command: "sudo runuser -l cytric -c '/home/ifao/bin/bnf.sh ${action}'"
    }
}
def extract_dist(Node, extractDist, distDir, cytricVersion, action) {
    def remote = [:]
    remote.name = "${Node}"
    remote.host = "${Node}"
    remote.allowAnyHosts = true
    withCredentials([sshUserPrivateKey(credentialsId: 'jenkins-ssh-user', keyFileVariable: 'identity', passphraseVariable: 'passphrase', usernameVariable: 'userName')]) {
            remote.user = userName
            remote.identityFile = identity
            remote.passphrase = passphrase
            sshCommand remote: remote, command: "sudo runuser -l cytric -c '${extractDist} /home/ifao/distrib/cytric/${distDir}/cytric_dist-${cytricVersion}.tgz ${action}'"
    }
}
def cyt_dist(Node, distDir, cytricVersion) {
    def remote = [:]
    remote.name = "${Node}"
    remote.host = "${Node}"
    remote.allowAnyHosts = true
    withCredentials([sshUserPrivateKey(credentialsId: 'jenkins-ssh-user', keyFileVariable: 'identity', passphraseVariable: 'passphrase', usernameVariable: 'userName')]) {
            remote.user = userName
            remote.identityFile = identity
            remote.passphrase = passphrase
            sshPut remote: remote, from: "cytric_dist-${cytricVersion}.tgz", into: '/tmp/'
            sshCommand remote: remote, command: "chmod 644 /tmp/cytric_dist-${cytricVersion}.tgz"
            sshCommand remote: remote, command: "sudo -u cytric mkdir -p /home/ifao/distrib/cytric/${distDir}"
            sshCommand remote: remote, command: "sudo -u cytric cp -f /tmp/cytric_dist-${cytricVersion}.tgz /home/ifao/distrib/cytric/${distDir}/"
            sshCommand remote: remote, command: "rm -f /tmp/cytric_dist-${cytricVersion}.tgz"
    }
}
def cyt_stop(Node, cyt_script, script_stop_option) {
    def remote = [:]
    remote.name = "${Node}"
    remote.host = "${Node}"
    remote.allowAnyHosts = true
    withCredentials([sshUserPrivateKey(credentialsId: 'jenkins-ssh-user', keyFileVariable: 'identity', passphraseVariable: 'passphrase', usernameVariable: 'userName')]) {
            remote.user = userName
            remote.identityFile = identity
            remote.passphrase = passphrase
            sshCommand remote: remote, command: "sudo -u cytric ${cyt_script} ${script_stop_option}"
    }
}
def cyt_start(Node, cyt_script, cyt_home, BNF) {
    def remote = [:]
    remote.name = "${Node}"
    remote.host = "${Node}"
    remote.allowAnyHosts = true
    withCredentials([sshUserPrivateKey(credentialsId: 'jenkins-ssh-user', keyFileVariable: 'identity', passphraseVariable: 'passphrase', usernameVariable: 'userName')]) {
            remote.user = userName
            remote.identityFile = identity
            remote.passphrase = passphrase
            sshCommand remote: remote, command: "sudo runuser -l cytric -c 'cd ${cyt_home} && . set_classpath && sh ./scripts/dbunlocker.sh'"
            sshCommand remote: remote, command: "sudo -u cytric ${cyt_script} start"
            if (BNF == 'on') {
                sleep(120)
            } else {
                sleep(15)
            }
            cytStatus = sshCommand remote: remote, command: "sudo -u cytric ${cyt_script} status_simple"
    }
    if (cytStatus == 'OK') {
        echo "The cytric application is successfully started on ${Node}"
    } else {
        error("Please check cytric application on ${Node}")
    }
}
def updateVersion(Node, cyt_home) {
    echo "Executing updateVersion.sh on ${Node}"
    def remote = [:]
    remote.name = "${Node}"
    remote.host = "${Node}"
    remote.allowAnyHosts = true
    withCredentials([sshUserPrivateKey(credentialsId: 'jenkins-ssh-user', keyFileVariable: 'identity', passphraseVariable: 'passphrase', usernameVariable: 'userName')]) {
            remote.user = userName
            remote.identityFile = identity
            remote.passphrase = passphrase
            sshCommand remote: remote, command: "sudo runuser -l cytric -c 'cd ${cyt_home} && . set_classpath && sh ./scripts/dbunlocker.sh'"
            sshCommand remote: remote, command: "sudo runuser -l cytric -c 'cd ${cyt_home} && . set_classpath && sh ./updateVersion.sh'"
    }
}
def logrotate(Node, logrotate_cytric, logrotate_search) {
    echo "Executing  log rotate on ${Node}"
    def remote = [:]
    remote.name = "${Node}"
    remote.host = "${Node}"
    remote.allowAnyHosts = true
    withCredentials([sshUserPrivateKey(credentialsId: 'jenkins-ssh-user', keyFileVariable: 'identity', passphraseVariable: 'passphrase', usernameVariable: 'userName')]) {
            remote.user = userName
            remote.identityFile = identity
            remote.passphrase = passphrase
            sshCommand remote: remote, command: "sudo -u cytric ${logrotate_search} > /dev/null 2>&1"
            sshCommand remote: remote, command: "sudo -u cytric ${logrotate_cytric} > /dev/null 2>&1"
    }
}
def db_changes(passiveNode, activeNode, cyt_home) {
    echo "Checking DB version in the file ${cyt_home}/db-version.properties on passive NODE: ${passiveNode}"
    def remote = [:]
    remote.name = "${passiveNode}"
    remote.host = "${passiveNode}"
    remote.allowAnyHosts = true
    withCredentials([sshUserPrivateKey(credentialsId: 'jenkins-ssh-user', keyFileVariable: 'identity', passphraseVariable: 'passphrase', usernameVariable: 'userName')]) {
            remote.user = userName
            remote.identityFile = identity
            remote.passphrase = passphrase
            dbpassive = sshCommand remote: remote, command: "sudo -u cytric grep 'ibe.db.version' ${cyt_home}/db-version.properties"
            echo "DB version in the ${cyt_home}/db-version.properties on $passiveNode is: $dbpassive"
    }
    echo "Checking DB version in the file ${cyt_home}/db-version.properties on active NODE: ${activeNode}"
    remote.name = "${activeNode}"
    remote.host = "${activeNode}"
    remote.allowAnyHosts = true
    withCredentials([sshUserPrivateKey(credentialsId: 'jenkins-ssh-user', keyFileVariable: 'identity', passphraseVariable: 'passphrase', usernameVariable: 'userName')]) {
            remote.user = userName
            remote.identityFile = identity
            remote.passphrase = passphrase
            dbactive = sshCommand remote: remote, command: "sudo -u cytric grep 'ibe.db.version' ${cyt_home}/db-version.properties"
            echo "DB version in the ${cyt_home}/db-version.properties on $activeNode is: $dbactive"
    }
    if (dbactive != dbpassive) {
        error('The DB changes has been found')
        } else {
        echo 'ALL are OK. No DB changes has been found.'
    }
}
def bnf_status(passiveNode, activeNode, cyt_home) {
    def remote = [:]
    remote.name = "${passiveNode}"
    remote.host = "${passiveNode}"
    remote.allowAnyHosts = true
    withCredentials([sshUserPrivateKey(credentialsId: 'jenkins-ssh-user', keyFileVariable: 'identity', passphraseVariable: 'passphrase', usernameVariable: 'userName')]) {
            remote.user = userName
            remote.identityFile = identity
            remote.passphrase = passphrase
            bnf_passive = sshCommand remote: remote, command: "sudo -u cytric grep 'net.ifao' ${cyt_home}/WEB-INF/classes/META-INF/services/net.ifao.cluster.node.NodeProvider"
    }
    remote.name = "${activeNode}"
    remote.host = "${activeNode}"
    remote.allowAnyHosts = true
    withCredentials([sshUserPrivateKey(credentialsId: 'jenkins-ssh-user', keyFileVariable: 'identity', passphraseVariable: 'passphrase', usernameVariable: 'userName')]) {
            remote.user = userName
            remote.identityFile = identity
            remote.passphrase = passphrase
            bnf_active = sshCommand remote: remote, command: "sudo -u cytric grep 'net.ifao' ${cyt_home}/WEB-INF/classes/META-INF/services/net.ifao.cluster.node.NodeProvider"
    }
    return [bnf_passive, bnf_active]
}
def return_enviroments(Environment, Application, email_prod, email_qa) {
    def node1 = ''
    def node2 = ''
    def cytPort = '9443'
    def searchPort = '8080'
    def haproxy = ''
    def hanode1 = ''
    def hanode2 = ''
    def cyt_habackend = ''
    def search_habackend = ''
    def cyt_script = '/home/ifao/bin/cytricv7'
    def search_script = '/home/ifao/bin/search'
    def cyt_home = '/home/ifao/cytric'
    def script_stop_option = 'stop_planned'
    def cytDistUrl = 'https://nexus.secure.ifao.net:8443/repository/cytric-release-maven2/net/ifao/cytric/ibetms/cytric_dist/'
    def extractDist = '/home/ifao/bin/extractdist.sh'
    def logrotate_cytric = '/home/ifao/bin/logrotate-cytric.sh'
    def logrotate_search = '/home/ifao/bin/logrotate-search.sh'
    def recipient = ''
    if (Environment == 'QA') {
        haproxy = 'hap-test.sofia.ifao.net'
        script_stop_option = 'stop'
        recipient = "${email_qa}"
        email_error = "${email_qa}"
        } else if (Environment == 'STAGING' || params.Environment == 'PROD') {
        haproxy = 'haproxy.secure.ifao.net'
        recipient = "${email_prod}"
        email_error = "${email_prod}"
        } else {
        error('Please select Environment')
    }
    if (Application == 'MONTBLANC') {
        node1 = 'vmmontblanc1.sofia.ifao.net'
        node2 = 'vmmontblanc11.sofia.ifao.net'
        hanode1 = 'vmmontblanc1'
        hanode2 = 'vmmontblanc11'
        cytPort = '9292'
        searchPort = '7180'
        cyt_habackend = 'montblanc-ha'
        search_habackend = 'montblanc-ha-search'
        cyt_home = '/home/ifao/montblanc'
        extractDist = '/home/ifao/bin/extractdistMontblanc.sh'
        cytDistUrl = 'ftp://bush.sofia.ifao.net/pub/cytric/dist/backup-nodes'
        logrotate_cytric = '/home/ifao/bin/logrotate-montblanc.sh'
        logrotate_search = '/home/ifao/bin/logrotate-montblanc_search.sh'
        } else if (Application == 'JAKARTA') {
        node1 = 'jakarta1.sofia.ifao.net'
        node2 = 'jakarta11.sofia.ifao.net'
        hanode1 = 'jakarta1'
        hanode2 = 'jakarta11'
        cytPort = '7443'
        searchPort = '8001'
        cyt_habackend = 'jakarta-ha'
        search_habackend = 'jakarta-ha-search'
        extractDist = '/home/ifao/bin/extractdistJakarta.sh'
        logrotate_cytric = '/home/ifao/bin/logrotate-jakarta_cytric.sh'
        logrotate_search = '/home/ifao/bin/logrotate-jakarta_search.sh'
        } else if (Application == 'JAKARTAPATCH') {
        node1 = 'jakartapatch1.sofia.ifao.net'
        node2 = 'jakartapatch11.sofia.ifao.net'
        hanode1 = 'jakartapatch1'
        hanode2 = 'jakartapatch11'
        cytPort = '9080'
        searchPort = '8002'
        cyt_habackend = 'jakartapatch-ha'
        search_habackend = 'jakartapatch-ha-search'
        cyt_home = '/home/ifao/jakartapatch'
        extractDist = '/home/ifao/bin/extractdistJakartapatch.sh'
        logrotate_cytric = '/home/ifao/bin/logrotate-jakartapatch_cytric.sh'
        logrotate_search = '/home/ifao/bin/logrotate-jakartapatch_search.sh'
        } else if (Application == 'FBP') {
        node1 = 'svfbpappcy01t.sofia.ifao.net'
        node2 = 'svfbpappcy02t.sofia.ifao.net'
        hanode1 = 'svfbpappcy01t'
        hanode2 = 'svfbpappcy02t'
        searchPort = '8001'
        cyt_habackend = 'fbp-ha'
        search_habackend = 'fbp-ha-search'
        extractDist = '/home/ifao/bin/extractdistcytric.sh'
        } else if (Application == 'STAGING') {
        node1 = 'vmstaging1.secure.ifao.net'
        node2 = 'vmstaging11.secure.ifao.net'
        hanode1 = 'vmstaging1'
        hanode2 = 'vmstaging11'
        cyt_habackend = 'ha-staging'
        search_habackend = 'ha-staging-search'
        } else if (Application == 'WORLD') {
        node1 = 'vmworld1.secure.ifao.net'
        node2 = 'vmworld11.secure.ifao.net'
        hanode1 = 'vmworld1'
        hanode2 = 'vmworld11'
        cyt_habackend = 'ha-world'
        search_habackend = 'ha-world-search'
        } else if (Application == 'TRAVEL') {
        node1 = 'vmtravel1.secure.ifao.net'
        node2 = 'vmtravel11.secure.ifao.net'
        hanode1 = 'vmtravel1'
        hanode2 = 'vmtravel11'
        cyt_habackend = 'ha-travel'
        search_habackend = 'ha-travel-search'
        } else if (Application == 'MEGA') {
        node1 = 'vmmega1.secure.ifao.net'
        node2 = 'vmmega11.secure.ifao.net'
        hanode1 = 'vmmega1'
        hanode2 = 'vmmega11'
        cyt_habackend = 'ha-mega'
        search_habackend = 'ha-mega-search'
        } else if (Application == 'KALLISTO') {
        node1 = 'vmkallisto1.secure.ifao.net'
        node2 = 'vmkallisto11.secure.ifao.net'
        hanode1 = 'vmkallisto1'
        hanode2 = 'vmkallisto11'
        cyt_habackend = 'ha-kallisto'
        search_habackend = 'ha-kallisto-search'
        } else if (Application == 'AMADEUS') {
        node1 = 'vmamadeus1.secure.ifao.net'
        node2 = 'vmamadeus11.secure.ifao.net'
        hanode1 = 'vmamadeus1'
        hanode2 = 'vmamadeus11'
        cyt_habackend = 'ha-amadeus'
        search_habackend = 'ha-amadeus-search'
        } else if (Application == 'AMADEUS2') {
        node1 = 'vmamadeus2-1.secure.ifao.net'
        node2 = 'vmamadeus2-11.secure.ifao.net'
        hanode1 = 'vmamadeus2-1'
        hanode2 = 'vmamadeus2-11'
        cyt_habackend = 'ha-amadeus2'
        search_habackend = 'ha-amadeus2-search'
        } else if (Application == 'APAC') {
        node1 = 'vmamadeusapac1.secure.ifao.net'
        node2 = 'vmamadeusapac11.secure.ifao.net'
        hanode1 = 'vmamadeusapac1'
        hanode2 = 'vmamadeusapac11'
        cyt_habackend = 'ha-amadeusapac'
        search_habackend = 'ha-amadeusapac-search'
        } else if (Application == 'AMERICAS') {
        node1 = 'vmamadeusam1.secure.ifao.net'
        node2 = 'vmamadeusam11.secure.ifao.net'
        hanode1 = 'vmamadeusam1'
        hanode2 = 'vmamadeusam11'
        cyt_habackend = 'ha-amadeusam'
        search_habackend = 'ha-amadeusam-search'
        } else {
        error('Please select Application')
    }
    return [node1, node2, cytPort, searchPort, haproxy, hanode1, hanode2, cyt_habackend, search_habackend, cyt_script, search_script, cyt_home, script_stop_option, cytDistUrl, extractDist, logrotate_cytric, logrotate_search, recipient, email_error]
}
//
properties([
    parameters([
        [
            $class: 'ChoiceParameter',
            choiceType: 'PT_SINGLE_SELECT',
            description: 'Please select the Environment',
            filterLength: 1,
            filterable: false,
            name: 'Environment',
            script: [
                $class: 'GroovyScript',
                script: [
                    classpath: [],
                    sandbox: false,
                    script: '''\
                        return ["Select:selected", "QA", "STAGING", "PROD"]'''.stripIndent()
                ]
            ]
        ],
        [
            $class: 'CascadeChoiceParameter',
            choiceType: 'PT_SINGLE_SELECT',
            description: 'Please select the application',
            filterLength: 1,
            filterable: false,
            name: 'Application',
            referencedParameters: 'Environment',
            script: [
                $class: 'GroovyScript',
                script: [
                    classpath: [],
                    sandbox: false,
                    script: '''\
                        List QaList  = ["Select:selected", "MONTBLANC", "JAKARTA", "JAKARTAPATCH", "FBP"]
                        List StagingList  = ["Select:selected", "STAGING"]
                        List ProdList  = ["Select:selected", "WORLD", "TRAVEL", "MEGA", "KALLISTO", "AMADEUS", "AMADEUS2", "APAC", "AMERICAS"]
                        List default_item = ["None"]
                        if (Environment == 'QA') {
                        return QaList
                        } else if (Environment == 'STAGING') {
                        return StagingList
                        } else if (Environment == 'PROD') {
                        return ProdList
                        } else {
                        return default_item
                        }
                    '''.stripIndent()
                ]
            ]
        ],
        choice(
            name: 'BNF',
            choices: ['on', 'off'],
            description: 'Please choose if BNF functionality should be activated or deactivated'
        ),
        choice(
            name: 'UpdateCytric',
            choices: ['Yes', 'No'],
            description: 'Do you want to update cytric app on standby NODE'
        ),
        string(
            name: 'cytricVersion',
            description: 'Please enter cytric version if you want to update cytric app on standby NODE',
            trim: true
        )
    ])
])
//
pipeline {
    agent any
    stages {
        stage('Define Enviroments') {
            steps {
                script {
                    currentBuild.description = "${params.Application}"
                    (node1, node2, cytPort, searchPort, haproxy, hanode1, hanode2, cyt_habackend, search_habackend, cyt_script, search_script, cyt_home, script_stop_option, cytDistUrl, extractDist, logrotate_cytric, logrotate_search, recipient, email_error) = return_enviroments("${params.Environment}", "${params.Application}", "${email_prod}", "${email_qa}")
                }
            }
        }
        stage('Checking cytric NODEs status') {
            steps {
                script {
                    def node1status = ''
                    def node2status = ''
                    def response = ''
                    //
                    url = "https://$node1:$cytPort/nodestatus"
                    response = sh(script: "curl --insecure --connect-timeout 30 -s -o /dev/null $url", returnStatus: true, returnStdout: true)
                    if (response == 0) {
                        code = sh(script: "curl --insecure -sLI -o /dev/null -w '%{response_code}' $url", returnStdout: true)
                        echo "HTTP response status code from NODE1: $code"
                        if (code == '200') {
                            node1status = 'Active'
                            activeNode = "$node1"
                            activeHa = "$hanode1"
                        } else {
                            node1status = 'Passive'
                            passiveNode = "$node1"
                            passiveHa = "$hanode1"
                        }
                    } else {
                        node1status = 'Passive'
                        passiveNode = "$node1"
                        passiveHa = "$hanode1"
                    }
                    //
                    url = "https://$node2:$cytPort/nodestatus"
                    response = sh(script: "curl --insecure --connect-timeout 30 -s -o /dev/null $url", returnStatus: true, returnStdout: true)
                    if (response == 0) {
                        code = sh(script: "curl --insecure -sLI -o /dev/null -w '%{response_code}' $url", returnStdout: true)
                        echo "HTTP response status code from NODE2: $code"
                        if (code == '200') {
                            node2status = 'Active'
                            activeNode = "$node2"
                            activeHa = "$hanode2"
                        } else {
                            node2status = 'Passive'
                            passiveNode = "$node2"
                            passiveHa = "$hanode2"
                        }
                    } else {
                        node2status = 'Passive'
                        passiveNode = "$node2"
                        passiveHa = "$hanode2"
                    }
                    if (node1status == 'Active' || node2status == 'Active') {
                        echo "Status of $node1 = $node1status"
                        echo "Status of $node2 = $node2status"
                    } else {
                        error("No ACTIVE node is found. Please check status of $node1 and $node2 using console and execute manually pipeline for switching.")
                    }
                }
            }
        }
        stage('Ensure latest version of the cytric scripts on NODES') {
            steps {
                dir('cyt_cripts') {
                    echo 'Get cytric scripts from repository'
                    git branch: 'master', changelog: false, credentialsId: 'GIT_CREDS', poll: true, url: 'https://repository.secure.ifao.net:7443/scm/its/cytric_config.git'
                    script {
                        //cyt_scripts(passiveNode, activeNode, extractDist, Env, App)
                        cyt_scripts("${passiveNode}", "${activeNode}", "${extractDist}","${params.Environment}", "${params.Application}")
                    }
                }
            }
        }
        stage('Ensure lsyncd status on NODES') {
            failFast true
            parallel {
                stage('Ensure lsyncd is started on active NODE') {
                    steps {
                        script {
                            //lsyncd(Node, action)
                            lsyncd("${activeNode}", 'start')
                        }
                    }
                }
                stage('Ensure lsyncd is stopped on passive NODE') {
                    steps {
                        script {
                            //lsyncd(Node, action)
                            lsyncd("${passiveNode}", 'stop')
                        }
                    }
                }
            }
        }
        stage('Checking search application status on NODES') {
            failFast true
            parallel {
                stage('Checking search application status on active NODE') {
                    steps {
                        script {
                            echo "Checking status of search application on ${activeNode}"
                            def remote = [:]
                            remote.name = "${activeNode}"
                            remote.host = "${activeNode}"
                            remote.allowAnyHosts = true
                            withCredentials([sshUserPrivateKey(credentialsId: 'jenkins-ssh-user', keyFileVariable: 'identity', passphraseVariable: 'passphrase', usernameVariable: 'userName')]) {
                                remote.user = userName
                                remote.identityFile = identity
                                remote.passphrase = passphrase
                                searchStatusActive = sshCommand remote: remote, command: "sudo -u cytric ${search_script} status_simple"
                                echo "Status of search application is: ${searchStatusActive}"
                            }
                            if (searchStatusActive == 'OK') {
                                url = "http://${activeNode}:${searchPort}/svc/search/about"
                                response = sh(script: "curl $url | grep 'Initialized on'", returnStdout: true).trim()
                                if (response == 'Initialized on: -') {
                                    echo "The search application is NOT Initialized on ${activeNode}"
                                    searchInitActive = 'FAILED'
                                } else {
                                    echo "${response}"
                                    searchInitActive = 'OK'
                                }
                            }
                        }
                    }
                }
                stage('Checking search application status on passive NODE') {
                    steps {
                        script {
                            echo "Checking status of search application on ${passiveNode}"
                            def remote = [:]
                            remote.name = "${passiveNode}"
                            remote.host = "${passiveNode}"
                            remote.allowAnyHosts = true
                            withCredentials([sshUserPrivateKey(credentialsId: 'jenkins-ssh-user', keyFileVariable: 'identity', passphraseVariable: 'passphrase', usernameVariable: 'userName')]) {
                                remote.user = userName
                                remote.identityFile = identity
                                remote.passphrase = passphrase
                                searchStatusPassive = sshCommand remote: remote, command: "sudo -u cytric ${search_script} status_simple"
                                echo "Status of search application is: ${searchStatusPassive}"
                            }
                            if (searchStatusPassive == 'OK') {
                                url = "http://${passiveNode}:${searchPort}/svc/search/about"
                                response = sh(script: "curl $url | grep 'Initialized on'", returnStdout: true).trim()
                                if (response == 'Initialized on: -') {
                                    echo "The search application is NOT Initialized on ${activeNode}"
                                    searchInitPassive = 'FAILED'
                                } else {
                                    echo "${response}"
                                    searchInitPassive = 'OK'
                                }
                            }
                        }
                    }
                }
            }
        }
        stage('Ensure only one Initialized search app is enabled') {
            steps {
                script {
                    if (searchStatusActive == 'OK' && searchInitActive == 'OK') {
                        echo "Desable search on passive NODE: ${passiveNode} on HAproxy: ${haproxy} -> ${search_habackend} -> ${passiveHa}"
                        echo "Enable search on active NODE: ${activeNode} on HAproxy: ${haproxy} -> ${search_habackend} -> ${activeHa}"
                        def remote = [:]
                        remote.name = "${haproxy}"
                        remote.host = "${haproxy}"
                        remote.allowAnyHosts = true
                        withCredentials([sshUserPrivateKey(credentialsId: 'jenkins-ssh-user', keyFileVariable: 'identity', passphraseVariable: 'passphrase', usernameVariable: 'userName')]) {
                                remote.user = userName
                                remote.identityFile = identity
                                remote.passphrase = passphrase
                                sshCommand remote: remote, command: "echo 'enable server ${search_habackend}/${activeHa}' | sudo socat stdio /var/lib/haproxy/stats"
                                sshCommand remote: remote, command: "echo 'disable server ${search_habackend}/${passiveHa}' | sudo socat stdio /var/lib/haproxy/stats"
                        }
                    } else if (searchStatusPassive == 'OK' && searchInitPassive == 'OK') {
                        echo "Desable search on active NODE: ${activeNode} on HAproxy: ${haproxy} -> ${search_habackend} -> ${activeHa}"
                        echo "Enable search on passive NODE: ${passiveNode} on HAproxy: ${haproxy} -> ${search_habackend} -> ${passiveHa}"
                        def remote = [:]
                        remote.name = "${haproxy}"
                        remote.host = "${haproxy}"
                        remote.allowAnyHosts = true
                        withCredentials([sshUserPrivateKey(credentialsId: 'jenkins-ssh-user', keyFileVariable: 'identity', passphraseVariable: 'passphrase', usernameVariable: 'userName')]) {
                                remote.user = userName
                                remote.identityFile = identity
                                remote.passphrase = passphrase
                                sshCommand remote: remote, command: "echo 'enable server ${search_habackend}/${passiveHa}' | sudo socat stdio /var/lib/haproxy/stats"
                                sshCommand remote: remote, command: "echo 'disable server ${search_habackend}/${activeHa}' | sudo socat stdio /var/lib/haproxy/stats"
                        }
                    } else {
                        echo "The search application is NOT Initialized on both NODEs: ${activeNode} and ${passiveNode}"
                        error('Please check manually if search application is Initialized on less one node and start pipeline again')
                    }
                }
            }
        }
        stage('Downloading cytric distribution') {
            when {
                equals expected: 'Yes', actual: "${UpdateCytric}"
            }
            steps {
                dir('dist') {
                    script {
                        echo "Checking for cytric distribution on ${cytDistUrl}/${cytricVersion}/cytric_dist-${cytricVersion}.tgz"
                        url = "${cytDistUrl}/${cytricVersion}/cytric_dist-${cytricVersion}.tgz"
                        response = sh(script: "curl --insecure --connect-timeout 30 -s -o cytric_dist-${cytricVersion}.tgz $url", returnStatus: true, returnStdout: true)
                        if (response == 0) {
                            echo "cytric distribution cytric_dist-${cytricVersion}.tgz is found"
                        } else {
                            error("cytric_dist-${cytricVersion}.tgz distribution is not found.")
                        }
                    }
                }
            }
        }
        stage('Uploading cytric distribution on passive NODE') {
            when {
                equals expected: 'Yes', actual: "${UpdateCytric}"
            }
            steps {
                dir('dist') {
                    script {
                        echo "Uloading cytric distribution to passive NODE: ${passiveNode}"
                        def values = "${cytricVersion}".split('\\.')
                        distDir = values[0] + '.' + values[1] + '.x'
                        //cyt_dist(Node, distDir, cytricVersion)
                        cyt_dist("${passiveNode}", "${distDir}", "${cytricVersion}")
                    }
                }
            }
        }
        stage('Ensure cytric application is stopped on passive NODE') {
            when {
                equals expected: 'Yes', actual: "${UpdateCytric}"
            }
            steps {
                script {
                    echo "Force stop the cytric application on passive NODE: ${passiveNode}"
                    //cyt_stop(Node, cyt_script, script_stop_option)
                    cyt_stop("${passiveNode}", "${cyt_script}", "${script_stop_option}")
                }
            }
        }
        stage('Extracting cytric distribution on passive NODE') {
            when {
                equals expected: 'Yes', actual: "${UpdateCytric}"
            }
            steps {
                script {
                    echo "Extracting cytric_dist-${cytricVersion}.tgz on passive NODE ${passiveNode}"
                    if (params.BNF == 'on') {
                        //extract_dist(Node, extractDist, distDir, cytricVersion, action)
                        extract_dist("${passiveNode}", "${extractDist}", "${distDir}", "${cytricVersion}", ' bnf=on')
                    } else {
                        //extract_dist(Node, extractDist, distDir, cytricVersion, action)
                        extract_dist("${passiveNode}", "${extractDist}", "${distDir}", "${cytricVersion}", ' bnf=off')
                    }
                }
            }
        }
        stage('Checking for DB changes') {
            when {
                equals expected: 'Yes', actual: "${UpdateCytric}"
            }
            steps {
                script {
                    // db_changes(passiveNode, activeNode, cyt_home)
                    db_changes("${passiveNode}", "${activeNode}", "${cyt_home}")
                }
            }
        }
        stage('Set up BNF activation/deactivation on passive NODE') {
            steps {
                script {
                    if (params.BNF == 'on') {
                        echo "Activate BNF on passive NODE ${passiveNode}"
                        //setup_bnf(Node, action)
                        setup_bnf("${passiveNode}", 'bnf=on')
                    } else {
                        echo "Deactivate BNF on passive NODE ${passiveNode}"
                        //setup_bnf(Node, action)
                        setup_bnf("${passiveNode}", 'bnf=off')
                    }
                }
            }
        }
        stage('Current status of BNF on NODES') {
             when {
                equals expected: 'on', actual: "${BNF}"
            }
            steps {
                script {
                    //bnf_status(passiveNode, activeNode, cyt_home)
                    (bnf_passive, bnf_active) = bnf_status("${passiveNode}", "${activeNode}", "${cyt_home}")
                    if (bnf_passive != bnf_active) {
                        echo "Activating BNF using BNL workflow becuase on active node ${activeNode} BNF is not activated"
                        BNF='off'
                    }
                }
            }
        }
        stage('BNF - Executing updateVersion.sh on passive NODE') {
            when {
                equals expected: 'Yes', actual: "${UpdateCytric}"
                equals expected: 'on', actual: "${BNF}"
            }
            steps {
                script {
                    // updateVersion(Node, cyt_home)
                    updateVersion("${passiveNode}", "${cyt_home}")
                }
            }
        }
        stage('BNF - Starting cytric application and lsyncd on passive NODE') {
            when{
                equals expected: 'on', actual: "${BNF}"
            }
            steps {
                script {
                    echo "Starting cytric application and lsyncd on ${passiveNode}"
                    //cyt_start(Node, cyt_script, cyt_home, BNF)
                    cyt_start("${passiveNode}", " ${cyt_script}", "${cyt_home}", "${BNF}")
                    //lsyncd(Node, action)
                    lsyncd("${passiveNode}", 'start')
                }
            }
        }
        stage('BNF - Disable Sitescope monitors') {
            when{
                equals expected: 'on', actual: "${BNF}"
            }
            steps {
                dir('cyt') {
                    git branch: 'master', changelog: false, credentialsId: 'GIT_CREDS', poll: true, url: 'https://repository.secure.ifao.net:7443/scm/its/sitescope.git'
                    script {
                        sh "./sitescope.sh ${params.Environment} ${params.Application} 10 || true"
                    }
                }
            }
        }
        stage('BNF - Stopping cytric application and lsyncd on active NODE') {
            when{
                equals expected: 'on', actual: "${BNF}"
            }
            steps {
                script {
                    echo "Stopping cytric application and lsyncd on active NODE: ${activeNode}"
                    if (UpdateCytric == 'Yes') {
                        //send_email(body, subject, recipient)
                        send_email("The cytric application ${params.Application} on passive NODE ${passiveNode} will be updated with cytric v${cytricVersion}", "The cytric application ${params.Application} will be updated.", "${recipient}")
                    } else {
                        //send_email(body, subject, recipient)
                        send_email("The cytric application ${params.Application} will be switched to passive NODE: ${passiveNode}.", "The cytric application ${params.Application} will be switched to passive NODE.", "${recipient}")
                    }
                    //cyt_stop(Node, cyt_script, script_stop_option)
                    cyt_stop("${activeNode}", "${cyt_script}", "${script_stop_option}")
                    //lsyncd(Node, action)
                    lsyncd("${activeNode}", 'stop')
                }
            }
        }
        stage('BNL - Disable Sitescope monitors') {
            when{
                equals expected: 'off', actual: "${BNF}"
            }
            steps {
                dir('cyt') {
                    git branch: 'master', changelog: false, credentialsId: 'GIT_CREDS', poll: true, url: 'https://repository.secure.ifao.net:7443/scm/its/sitescope.git'
                    script {
                        sh "./sitescope.sh ${params.Environment} ${params.Application} 10 || true"
                    }
                }
            }
        }
        stage('BNL - Stopping cytric application and lsyncd on active NODE') {
            when {
                equals expected: 'off', actual: "${BNF}"
            }
            steps {
                script {
                    echo "Stopping cytric application and lsyncd on active NODE: ${activeNode}"
                    if (UpdateCytric == 'Yes') {
                        //send_email(body, subject, recipient)
                        send_email("The cytric application ${params.Application} on passive NODE ${passiveNode} will be updated with cytric v${cytricVersion}", "The cytric application ${params.Application} will be updated.", "${recipient}")
                    } else {
                        //send_email(body, subject, recipient)
                        send_email("The cytric application ${params.Application} will be switched to passive NODE: ${passiveNode}.", "The cytric application ${params.Application} will be switched to passive NODE.", "${recipient}")
                    }
                    //cyt_stop(Node, cyt_script, script_stop_option)
                    cyt_stop("${activeNode}", "${cyt_script}", "${script_stop_option}")
                    //lsyncd(Node, action)
                    lsyncd("${activeNode}", 'stop')
                }
            }
        }
        stage('BNL - Executing updateVersion.sh on passive NODE') {
            when {
                equals expected: 'Yes', actual: "${UpdateCytric}"
                equals expected: 'off', actual: "${BNF}"
            }
            steps {
                script {
                    // updateVersion(Node, cyt_home)
                    updateVersion("${passiveNode}", "${cyt_home}")
                }
            }
        }
        stage('BNL - Starting cytric application and lsyncd on passive NODE') {
            when{
                equals expected: 'off', actual: "${BNF}"
            }
            steps {
                script {
                    echo "Starting cytric application and lsyncd on ${passiveNode}"
                    //cyt_start(Node, cyt_script, cyt_home, BNF)
                    cyt_start("${passiveNode}", " ${cyt_script}", "${cyt_home}", "${BNF}")
                    //lsyncd(Node, action)
                    lsyncd("${passiveNode}", 'start')
                }
            }
        }
        stage('Cheking status of cytric application after switching to passive NODE') {
            steps {
                script {
                    url = "https://$passiveNode:$cytPort/monitor?version=true"
                    for (int i = 0; i < 30; ++i) {
                        response = sh(script: "curl --insecure --connect-timeout 30 -s -o /dev/null $url", returnStatus: true, returnStdout: true)
                        if (response == 0) {
                            code = sh(script: "curl --insecure -sLI -o /dev/null -w '%{response_code}' $url", returnStdout: true)
                            if (code == '200') {
                                echo "The cytric application is successfully started on $passiveNode"
                                if (UpdateCytric == 'Yes') {
                                    //send_email(body, subject, recipient)
                                    send_email("The cytric application ${params.Application} on passive NODE ${passiveNode} have been updated and started with cytric v${cytricVersion}", "The cytric application ${params.Application} have been updated.", "${recipient}")
                                } else {
                                    //send_email(body, subject, recipient)
                                    send_email("The cytric application ${params.Application} have been switched to passive NODE: ${passiveNode}.", "The cytric application ${params.Application} is switched to passive NODE.", "${recipient}")
                                }
                                i = 30
                            } else {
                                error("Please check cytric application on ${passiveNode}")
                            }
                        } else if (i == 29) {
                            error("Please check cytric application on ${passiveNode}")
                        }
                        sleep (10)
                    }
                }
            }
        }
        stage('Setting up cytric crontab') {
            steps {
                dir('cyt') {
                    echo 'Get cytric crontabs from repository'
                    git branch: 'master', changelog: false, credentialsId: 'GIT_CREDS', poll: true, url: 'https://repository.secure.ifao.net:7443/scm/its/cytric_config.git'
                    script {
                        //cyt_cron(passiveNode, activeNode, Env, App)
                        cyt_cron("${passiveNode}", "${activeNode}", "${params.Environment}", "${params.Application}")
                    }
                }
            }
        }
        stage('Starting search application on passive NODE') {
            when {
                equals expected: 'FAILED', actual: "${searchStatusPassive}"
                beforeInput true
            }
            steps {
                script {
                    //search_start(Node, search_script)
                    search_start("${passiveNode}", "${search_script}")
                }
            }
        }
        stage('Checking search application status on passive NODE') {
            options {
                timeout(time: 90, unit: 'MINUTES')
            }
            steps {
                script {
                    url = "http://${passiveNode}:${searchPort}/svc/search/about"
                    for (int i = 0; i < 6; ++i) {
                        response = sh(script: "curl --insecure --connect-timeout 30 -s -o /dev/null $url", returnStatus: true, returnStdout: true)
                        if (response == 0) {
                            code = sh(script: "curl --insecure -sLI -o /dev/null -w '%{response_code}' $url", returnStdout: true)
                            if (code == '200') {
                                echo "The search application is successfully started on $passiveNode"
                                i = 6
                            } else {
                                error("Please check search application on ${passiveNode}")
                            }
                        } else if (i == 5) {
                            error("Please check search application on ${passiveNode}")
                        }
                        sleep (10)
                    }
                    response = sh(script: "curl $url | grep 'Initialized on'", returnStdout: true).trim()
                    echo "$response"
                    if (response == 'Initialized on: -') {
                        while (response == 'Initialized on: -') {
                            response = sh(script: "curl $url | grep 'Initialized on'", returnStdout: true).trim()
                            sleep(60)
                        }
                        echo "$response"
                        emai_response = sh(script: "curl $url", returnStdout: true)
                        //send_email(body, subject, recipient)
                        send_email("${emai_response}", "The search application for ${params.Application} is initialized on passive NODE.", "${recipient}")
                    } else {
                        emai_response = sh(script: "curl $url", returnStdout: true)
                        //send_email(body, subject, recipient)
                        send_email("${emai_response}", "The search application for ${params.Application} is initialized on passive NODE.", "${recipient}")
                    }
                }
            }
        }
        stage('Switching search application to passive NODE') {
            steps {
                script {
                    echo "Enable search on passive NODE: ${passiveNode} on HAproxy: ${haproxy} -> ${search_habackend} -> ${passiveHa}"
                    echo "Desable search on active NODE: ${activeNode} on HAproxy: ${haproxy} -> ${search_habackend} -> ${activeHa}"
                    def remote = [:]
                    remote.name = "${haproxy}"
                    remote.host = "${haproxy}"
                    remote.allowAnyHosts = true
                    withCredentials([sshUserPrivateKey(credentialsId: 'jenkins-ssh-user', keyFileVariable: 'identity', passphraseVariable: 'passphrase', usernameVariable: 'userName')]) {
                        remote.user = userName
                        remote.identityFile = identity
                        remote.passphrase = passphrase
                        sshCommand remote: remote, command: "echo 'enable server ${search_habackend}/${passiveHa}' | sudo socat stdio /var/lib/haproxy/stats"
                        sshCommand remote: remote, command: "echo 'disable server ${search_habackend}/${activeHa}' | sudo socat stdio /var/lib/haproxy/stats"
                    }
                }
            }
        }
        stage('Stopping search application on active NODE') {
            steps {
                script {
                    // search_stop(Node, search_script, script_stop_option)
                    search_stop("${activeNode}", "${search_script}", "${script_stop_option}")
                }
            }
        }
        stage('Rotate application logs on active NODE') {
            steps {
                script {
                    // logrotate(Node, logrotate_cytric, logrotate_search)
                    logrotate("${activeNode}", "${logrotate_cytric}", "${logrotate_search}")
                }
            }
        }
    }
    post {
        always {
            echo 'Cleaning up the build folder'
            dir(BUILD_ID) {
                deleteDir()
            }
            dir("${BUILD_ID}@tmp") {
                deleteDir()
            }
            sh 'rm -rf *'
            sh 'rm -rf .git'
        }
        failure {
            // build_failed(email_error)
            build_failed("${email_error}")
        }
    }
}
