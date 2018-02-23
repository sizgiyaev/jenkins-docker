#!groovy

import hudson.security.*
import hudson.security.csrf.DefaultCrumbIssuer
import hudson.util.*
import jenkins.model.*
import jenkins.install.*
import jenkins.security.s2m.*
import com.michelin.cio.hudson.plugins.rolestrategy.*

ERROR = 3
WARNING = 4
INFO = 6

def instance = Jenkins.getInstance()
def env = System.getenv()
def setupCompletedFile = env.JENKINS_HOME + '/initial_setup_completed'


if (isInitialSetup(setupCompletedFile)) {

    try {
        assert env.JENKINS_AUTHENTICATION
    } catch (AssertionError e) {
        println assertErrorNormalizedMessage(e.getLocalizedMessage())
        println logMessage('Jenkins exited.', INFO)
        System.exit(1)
    }

    switch (env.JENKINS_AUTHENTICATION.toLowerCase()) {
        
        case 'local':
            println logMessage('Configuring local authentication...', INFO)
            try {
                assert env.JENKINS_LOCAL_USER
                assert env.JENKINS_LOCAL_PASS
            } catch (AssertionError e) {
                println(assertErrorNormalizedMessage(e.getLocalizedMessage()))
                println logMessage('Jenkins exited.', INFO)
                System.exit(1)
            }
            setLocalAuthetication(env.JENKINS_LOCAL_USER, env.JENKINS_LOCAL_PASS, instance)
            setInitialSetupCompleted(setupCompletedFile)
            break
        
        case ~/ldap(_restricted)?/:
            println logMessage('Configuring LDAP authentication...', INFO)
            def restricted = false
            try {
                // Mandatory config
                assert env.JENKINS_LDAP_SERVER
                assert env.JENKINS_LDAP_ROOTDN
                assert env.JENKINS_LDAP_BIND_USER
                assert env.JENKINS_LDAP_BIND_PASS
                if (env.JENKINS_AUTHENTICATION.toLowerCase() == 'ldap_restricted') {
                    assert env.JENKINS_LDAP_ADMINS
                    restricted = true
                }
            } catch (AssertionError e) {
                println(assertErrorNormalizedMessage(e.getLocalizedMessage()))
                println logMessage('Jenkins exited.', INFO)
                System.exit(1)
            }
            
            // Optional config
            def sb = (env.JENKINS_SEARCH_BASE) ? env.JENKINS_SEARCH_BASE : ''
            def sf = (env.JENKINS_SEARCH_FILTER) ? env.JENKINS_SEARCH_FILTER : 'uid={0}'
            def dn = (env.JENKINS_DISPLAY_NAME_ATTRIBUTE) ? env.JENKINS_DISPLAY_NAME_ATTRIBUTE : 'displayname'
            def em = (env.JENKINS_EMAIL_ADDRESS_ATTRIBUTE) ? env.JENKINS_EMAIL_ADDRESS_ATTRIBUTE : 'mail'
            def gs = (env.JENKINS_GROUP_SEARCH) ? env.JENKINS_GROUP_SEARCH : ''
            def ir = (env.JENKINS_INHIBIT_INFER_ROOTDN) ? true : false
            def la = (env.JENKINS_LDAP_ADMINS) ? env.JENKINS_LDAP_ADMINS : ''

            def config = [
                server: env.JENKINS_LDAP_SERVER,
                rootdn: env.JENKINS_LDAP_ROOTDN,
                bind_user: env.JENKINS_LDAP_BIND_USER,
                bind_pass: env.JENKINS_LDAP_BIND_PASS,
                ldap_admins: la,
                search_base: sb,
                search_filter: sf,
                group_search: gs,
                display_attr: dn,
                email_attr: em,
                inhibit_infer_rootdn: ir

            ]
            setLDAPAuthentication(config, instance, restricted)
            break
        default:
            println errorMessage('No authentication method provided.')
            println infoMessage('Jenkins exited.')
            System.exit(1)
    }
    configureCommonSecurity(instance)
} else {
    infoMessage('The initial setup is already done.')
}


def setLocalAuthetication(username, password, instance) {
    
    def hudsonRealm = new HudsonPrivateSecurityRealm(false)
    def users = hudsonRealm.getAllUsers()

    if (username in users) {
        println infoMessage("The user " + username + " already exists. Updating password...")
        
        def user = hudson.model.User.get(username);
        def pass = hudson.security.HudsonPrivateSecurityRealm.Details.fromPlainPassword(password)
        user.addProperty(pass)
        user.save()
    } else {
        println logMessage("Creating local user: " + username + "...", INFO)

        hudsonRealm.createAccount(username, password)
        instance.setSecurityRealm(hudsonRealm)

        def strategy = new FullControlOnceLoggedInAuthorizationStrategy()
        strategy.setAllowAnonymousRead(false)
        instance.setAuthorizationStrategy(strategy)
        instance.save()
    }
}

def setLDAPAuthentication(config, instance, restricted) {
    SecurityRealm ldap_realm = new LDAPSecurityRealm(
            config['server'],
            config['rootdn'], 
            config['search_base'], 
            config['search_filter'], 
            config['group_search'],
            config['bind_user'], 
            config['bind_pass'], 
            config['inhibit_infer_rootdn']
    ) 
    instance.setSecurityRealm(ldap_realm)
    def strategy = null
    if (restricted) {
        strategy = new RoleBasedAuthorizationStrategy()
        assignAdminRole(config['ldap_admins'].replace(' ', '').split(','), strategy, instance)
    } else {
        strategy = new FullControlOnceLoggedInAuthorizationStrategy()
        strategy.setAllowAnonymousRead(false)
    }
    instance.setAuthorizationStrategy(strategy)
    instance.save()
}

def assignAdminRole (members, strategy, instance) {
    def adminPermissions = [
        "hudson.model.Hudson.Administer",
        "hudson.model.Hudson.Read"
    ]
    Set<Permission> adminPermissionSet = new HashSet<Permission>();
    adminPermissions.each { p ->
        def permission = Permission.fromId(p);
        if (permission != null) {
            adminPermissionSet.add(permission);
        } else {
            println logMessage("${p} is not a valid permission ID (ignoring)", WARNING)
        }
    }

    Role adminRole = new Role('admin', adminPermissionSet);
    strategy.addRole(RoleBasedAuthorizationStrategy.GLOBAL, adminRole);

    members.each { member ->
        println logMessage("Granting admin to ${member}", INFO)
        strategy.assignRole(RoleBasedAuthorizationStrategy.GLOBAL, adminRole, member);
    }
}

def configureCommonSecurity(def instance) {
    // Disable old Non-Encrypted protocols
    HashSet<String> newProtocols = new HashSet<>(instance.getAgentProtocols());
    newProtocols.removeAll(Arrays.asList(
        "JNLP3-connect", "JNLP2-connect", "JNLP-connect", "CLI-connect"
    ));
    instance.setAgentProtocols(newProtocols);
    
    // Disable remoting
    instance.getDescriptor("jenkins.CLI").get().setEnabled(false)

    // Enable Agent to master security subsystem
    instance.injector.getInstance(AdminWhitelistRule.class).setMasterKillSwitch(false);

    // Disable jnlp
    instance.setSlaveAgentPort(-1);

    // Enable CSRF
    instance.setCrumbIssuer(new DefaultCrumbIssuer(true))
       
    instance.save()
}

def assertErrorNormalizedMessage(def rawMessage) {
    errorObjectName = (rawMessage =~ /assert\s*env\.(.+).*/)[0][1]
    return  logMessage("The variable " + errorObjectName + " is not set.", ERROR)
}

def logMessage(def message, severity) {
    messageLevel = 'INFO'
    switch(severity) {
        case ERROR: 
            messageLevel = 'ERROR'
            break
        case WARNING:
            messageLevel = 'WARNING'
            break
    }
    
    return this.class.getName() + ":[" + messageLevel + "] " + message  
}

def setInitialSetupCompleted(def file) {
    def f = new File(file)
    if (!f.exists()) {
        f.write(new Date().format("HH:mm:ss dd/MM/yyyy"))
    }
}

def isInitialSetup(def file) {
    return !(new File(file).exists())
}
