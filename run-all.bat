@echo off
echo ======================================================================
echo           Enterprise Event-Driven Microservices Starter
echo ======================================================================
echo.
echo 1. Building all Maven modules...
call mvn clean install -DskipTests
if %ERRORLEVEL% neq 0 (
    echo [ERROR] Maven build failed. Please ensure JDK 17+ and Maven are installed.
    pause
    exit /b %ERRORLEVEL%
)

echo.
echo 2. Starting Infrastructure (PostgreSQL + Kafka)...
echo [INFO] Standing up containers via Docker Compose...
docker-compose up -d
if %ERRORLEVEL% neq 0 (
    echo [WARNING] Docker Compose failed to start. 
    echo.
    echo [ALTERNATIVE] If you don't have Docker running, you can run services standalone 
    echo using the in-memory H2 database by adding the 'local' profile!
    echo Example: cd user-service && mvn spring-boot:run -Dspring-boot.run.profiles=local
    echo.
    pause
)

echo.
echo 3. Launching Microservices in separate terminal windows...

echo Starting User Service (Port 8081)...
start "User Service [8081]" cmd /k "cd user-service && mvn spring-boot:run"
timeout /t 4 /nobreak > nul

echo Starting Product Service (Port 8082)...
start "Product Service [8082]" cmd /k "cd product-service && mvn spring-boot:run"
timeout /t 4 /nobreak > nul

echo Starting Order Service (Port 8083)...
start "Order Service [8083]" cmd /k "cd order-service && mvn spring-boot:run"
timeout /t 4 /nobreak > nul

echo Starting Payment Service (Port 8084)...
start "Payment Service [8084]" cmd /k "cd payment-service && mvn spring-boot:run"
timeout /t 4 /nobreak > nul

echo Starting Notification Service (Port 8085)...
start "Notification Service [8085]" cmd /k "cd notification-service && mvn spring-boot:run"

echo.
echo ======================================================================
echo All microservices are launching!
echo.
echo Access URLs:
echo - User Service:         http://localhost:8081/api/users
echo - Product Service:      http://localhost:8082/api/products
echo - Order Service:        http://localhost:8083/api/orders
echo - Payment Service:      http://localhost:8084/api/payments
echo - Notification Service: http://localhost:8085/api/notifications
echo ======================================================================
pause
