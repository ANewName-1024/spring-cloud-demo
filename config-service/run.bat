@echo off
set DB_HOST=8.137.116.121
set DB_PASSWORD=NewPass2024
set EUREKA_PASSWORD=EurekaNew2024
set JWT_SECRET=ConfigServiceJWTSecret2024

cd /d D:\
cd .openclaw\workspace\java_project\springcloud-demo\config-service
mvn spring-boot:run
pause
