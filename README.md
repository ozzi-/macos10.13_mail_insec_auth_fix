# Apple Mail Fix - 10.13
See https://discussions.apple.com/thread/8087473 for infos.

This java programm will manipulate your Apple Mail Account in a way to allow insecure authentication.
Furtheremore, there is a crude PHP tool that does the same.

## Manual steps
0. Setup an account in Mail up to the point where you need to tick the allow insecure flag and save
1. pkill Mail
2. open with a SQLite Browser (such as sqlitebrowser.org) ~/Library/Accounts/Accounts4.sqlite
3. From ZACCOUNT table find the account you want to fix and note the Z_PK and ZACCOUNTTYPE of it
4. From the ZACCOUNTTYPE table get all rows that have ZKEY = PortNumber, DisableDynamicConfiguration or AllowsInsecureAuthentication and where ZWONER is Z_PK of ZACCOUNT
5. Insert into ZACCOUNTPROPERTY Z_ENT=3,Z_OPT=1,ZOWNER=Z_PK of ZACCOUNT, ZKEY = ortNumber, DisableDynamicConfiguration or AllowsInsecureAuthentication, ZVALUE a blob containing the value (see examples)
6. Start Mail
