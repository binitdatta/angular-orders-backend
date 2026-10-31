#!/usr/bin/env python3
"""Development-only: add/update confidential Postman client and service account roles in EXISTING realm."""
import json,os,sys,urllib.request,urllib.parse,urllib.error,getpass
base=os.getenv('KEYCLOAK_URL','http://localhost:8080').rstrip('/')
realm=os.getenv('KEYCLOAK_REALM','bootcamp-orders')
user=os.getenv('KEYCLOAK_ADMIN_USER','admin')
pwd=os.getenv('KEYCLOAK_ADMIN_PASSWORD') or getpass.getpass('Keycloak master admin password: ')
client_id='postman-orders-m2m'
def call(method,path,token=None,payload=None,form=False):
 url=base+path
 data=None
 headers={'Accept':'application/json'}
 if token: headers['Authorization']='Bearer '+token
 if payload is not None:
  if form:
   data=urllib.parse.urlencode(payload).encode();headers['Content-Type']='application/x-www-form-urlencoded'
  else:
   data=json.dumps(payload).encode();headers['Content-Type']='application/json'
 try:
  with urllib.request.urlopen(urllib.request.Request(url,data=data,headers=headers,method=method),timeout=15) as r:
   b=r.read();return json.loads(b) if b else None
 except urllib.error.HTTPError as e:
  raise RuntimeError(f'{method} {path} failed (HTTP {e.code}): {e.read().decode()[:750]}') from e
try:
 token=call('POST','/realms/master/protocol/openid-connect/token',payload={'grant_type':'password','client_id':'admin-cli','username':user,'password':pwd},form=True)['access_token']
 prefix='/admin/realms/'+urllib.parse.quote(realm,safe='')
 clients=call('GET',prefix+'/clients?clientId='+urllib.parse.quote(client_id),token)
 cfg={'clientId':client_id,'name':'Postman Orders M2M (development only)','enabled':True,'protocol':'openid-connect','publicClient':False,'clientAuthenticatorType':'client-secret','standardFlowEnabled':False,'implicitFlowEnabled':False,'directAccessGrantsEnabled':False,'serviceAccountsEnabled':True,'fullScopeAllowed':True}
 if not clients:
  call('POST',prefix+'/clients',token,cfg)
  clients=call('GET',prefix+'/clients?clientId='+urllib.parse.quote(client_id),token)
 else:
  cid=clients[0]['id'];call('PUT',prefix+'/clients/'+cid,token,{**clients[0],**cfg})
 cid=next(c['id'] for c in clients if c['clientId']==client_id)
 mapper_path=prefix+'/clients/'+cid+'/protocol-mappers/models'
 mappers=call('GET',mapper_path,token)
 if not any(m.get('name')=='orders-api audience' for m in mappers):
  call('POST',mapper_path,token,{'name':'orders-api audience','protocol':'openid-connect','protocolMapper':'oidc-audience-mapper','consentRequired':False,'config':{'included.client.audience':'orders-api','access.token.claim':'true','id.token.claim':'false'}})
 service_user=call('GET',prefix+'/clients/'+cid+'/service-account-user',token)
 if not service_user or not service_user.get('id'):raise RuntimeError('Service account user is unavailable; check Service Accounts Enabled.')
 suid=service_user['id']
 roles=[]
 for name in ('CUSTOMER','ADMIN'):
  role=call('GET',prefix+'/roles/'+name,token)
  roles.append({'id':role['id'],'name':role['name']})
 call('POST',prefix+'/users/'+suid+'/role-mappings/realm',token,roles)
 secret=call('GET',prefix+'/clients/'+cid+'/client-secret',token)['value']
 print('\nSUCCESS: '+client_id+' created/updated in realm '+realm)
 print('Service account:', service_user.get('username'), '(CUSTOMER and ADMIN roles assigned)')
 print('Audience mapper: orders-api')
 print('Client secret (copy into Postman m2mClientSecret variable; do not commit/share):')
 print(secret)
 print('Token URL:',base+'/realms/'+realm+'/protocol/openid-connect/token')
except Exception as e:
 print('ERROR:',e,file=sys.stderr);sys.exit(1)
