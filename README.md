## Automated Jenkins Master Docker
----------
### Features:
1. Jenkins plugin list can be supplied at a time of container building
2. Authetication method can be defined at a time of container building  
    a. Local (The admin username and password must be supplied)  
    b. LDAP (all LDAP users will have admin rights to the newly deployed Jenkins)  
    c. LDAP restricted (LDAP username or group must be provided to restrict admin rights only to them for the newly deployed Jenkins)  

### Usage:  

#### Run container using install plugins from file option
```
docker run -d -p 8080:8080 -p 50000:50000 \
-v $(pwd)/plugins.sh:/tmp/plugins.sh jenkins-master --plugins-file /tmp/plugins.txt
```  

#### Run container using install plugins from environment variables (It's possible to mix options)

```
docker run -d -p 8080:8080 -p 50000:50000 -e JENKINS_PLUGIN_a=<plugin_b_id> \
                           -e JENKINS_PLUGIN_b=<plugin_a_id> \
                           sizgiyaev/jenkins --plugins-from-environment
```

#### Local authentication. The specified user will be the first admin on the system.
```
docker run -d -p 8080:8080 -p 50000:50000 \
-e 'JENKINS_AUTHENTICATION=local' \
-e 'JENKINS_LOCAL_USER=<username>' \
-e 'JENKINS_LOCAL_PASS=<password>' sizgiyaev/jenkins
```

#### LDAP authentication - Unrestricted Mode. All LDAP autheticated users will have admin privileges on the system.
```
docker run -it -p 8080:8080 -p 50000:50000 \
-e 'JENKINS_AUTHENTICATION=ldap' \
-e 'JENKINS_LDAP_SERVER=<ldap://{HOSTNAME}:{PORT}>' \
-e 'JENKINS_LDAP_ROOTDN=DC=domain,DC=com' \
-e 'JENKINS_LDAP_BIND_USER=<Manager user DN>' \
-e 'JENKINS_LDAP_BIND_PASS=<Manager user password>' \
-e 'JENKINS_SEARCH_FILTER=samaccountname={0}' \
sizgiyaev/jenkins

```

#### LDAP authentication - Restricted Mode. Only specified LDAP users or members of specified LDAP group will have admin privileges on the system.
```
docker run -it -p 8080:8080 -p 50000:50000 \
-e 'JENKINS_AUTHENTICATION=ldap_restricted' \
-e 'JENKINS_LDAP_SERVER=<ldap://{HOSTNAME}:{PORT}>' \
-e 'JENKINS_LDAP_ROOTDN=DC=domain,DC=com' \
-e 'JENKINS_LDAP_BIND_USER=<Manager user DN>' \
-e 'JENKINS_LDAP_BIND_PASS=<Manager user password>' \
-e 'JENKINS_SEARCH_FILTER=samaccountname={0}' \
-e 'JENKINS_LDAP_ADMINS=user1,group1,user2,group2' \
sizgiyaev/jenkins

```

### Parameters:

<span style="color:blue">**JENKINS_AUTHENTICATION**</span> - (Required) Authentication type. Can be one of ***local, ldap*** or ***ldap_restricted***.

#### Local authentication mode:
<span style="color:blue">**JENKINS_LOCAL_USER**</span>=<username> - (Required) Local admin username.   
<span style="color:blue">**JENKINS_LOCAL_PASS**</span> - (Required) Local admin user's password.  

#### LDAP authentication mode:
<span style="color:blue">**JENKINS_LDAP_SERVER**</span> - (Required) LDAP server URL in format *`ldaps?://{hostname}:{port}`*  
<span style="color:blue">**JENKINS_LDAP_ROOTDN**</span> - (Required) Base DN, eg. *`DC=domain,DC=com`*  
<span style="color:blue">**JENKINS_LDAP_BIND_USER**</span> - (Required) DN for LDAP manager user.  
<span style="color:blue">**JENKINS_LDAP_BIND_PASS**</span> - (Required) Password of LDAP manager user.  
<span style="color:blue">**JENKINS_LDAP_RESTRICTED**</span> - (Required) Defines the LDAP mode. Can be one of ***yes*** or ***no***.  
<span style="color:blue">**JENKINS_SEARCH_BASE**</span> - (Optional) Where to search users in LDAP tree, eg. *`ou=people`*   
<span style="color:blue">**JENKINS_SEARCH_FILTER**</span> - (Optional) Which LDAP attribute to use to identify username. Default is *`uid={0}`*. For Microsoft AD *`samaccountname={0}`* may be used.  
<span style="color:blue">**JENKINS_DISPLAY_NAME_ATTRIBUTE**</span> - (Optional) Display Name LDAP attribute. Default is *`displayname`*.  
<span style="color:blue">**JENKINS_EMAIL_ADDRESS_ATTRIBUTE**</span> - (Optional) Email Address LDAP attribute. Default is *`mail`*.  
<span style="color:blue">**JENKINS_GROUP_SEARCH**</span> - (Optional) Where to search users in LDAP tree, eg. *`ou=groups`*   
<span style="color:blue">**JENKINS_LDAP_ADMINS**</span> - (Required for restricted mode) - Comma separated list of users and/or groups to assign admin role.  

