import rsa
import base64
import requests
token = input('token: ')
challenge = input('challenge: ')
validate = input('validate: ')
key = rsa.PublicKey.load_pkcs1_openssl_pem("-----BEGIN PUBLIC KEY-----\nMIGfMA0GCSqGSIb3DQEBAQUAA4GNADCBiQKBgQDjb4V7EidX/ym28t2ybo0U6t0n\n6p4ej8VjqKHg100va6jkNbNTrLQqMCQCAYtXMXXp2Fwkk6WR+12N9zknLjf+C9sx\n/+l48mjUU8RqahiFD1XT/u2e0m2EN029OhCgkHx3Fc/KlFSIbak93EH/XlYis0w+\nXl69GV6klzgxW6d2xQIDAQAB\n-----END PUBLIC KEY-----\n")
thash = requests.get('https://passport.bilibili.com/x/passport-login/web/key', headers={
    'user-agent': 'qwq/1.1 awa/22.33 sx/0.1'
}).json()['data']['hash']
ePw = rsa.encrypt((thash+'thisismyppassword').encode(), key)
b64p = base64.b64encode(ePw).decode()
print('curl -X POST "https://passport.bilibili.com/x/passport-login/web/login" \\')
print('--data-urlencode "username=thisismy@e.mail" \\')
print('--data-urlencode "password='+b64p+'" \\')
print("--data-urlencode 'keep=0' --data-urlencode 'source=main_web' \\")
print("--data-urlencode 'token="+token+"' \\")
print("--data-urlencode 'challenge="+challenge+"' \\")
print("--data-urlencode 'validate="+validate+"' \\")
print("--data-urlencode 'seccode="+validate+"|jordan' -i")
