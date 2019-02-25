# Distributed Class Management System
A distributed system using IDL and Java CORBA framework, with hybrid UDP communication between servers.

# Usage:

1. Start Java orbd
```bash
start orbd -ORBInitialPort 1050
```

2. Start Center Server
```bash
java CenterServer -ORBInitialPort 1050 -ORBInitialHost localhost
```

3. Start Manger Client
```bash
java ManagerClient -ORBInitialPort 1050 -ORBInitialHost localhost
```
