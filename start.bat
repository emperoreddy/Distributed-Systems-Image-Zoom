@echo off
REM Set repository URL and target folder name
set "REPO_URL=https://github.com/emperoreddy/Distributed-Systems-Image-Zoom"
set "REPO_DIR=project"

REM Check if the repository directory exists
if exist "%REPO_DIR%" (
    echo Repository folder "%REPO_DIR%" exists. Updating repository...
    cd "%REPO_DIR%"
    git pull
    cd ..
) else (
    echo Cloning repository into "%REPO_DIR%"...
    git clone %REPO_URL% %REPO_DIR%
)

REM Change directory into the repository folder
cd "%REPO_DIR%"

REM Check if docker-compose.yml exists in the repository
if not exist "docker-compose.yml" (
    echo ERROR: docker-compose.yml not found in the repository.
    pause
    exit /b 1
)

REM Build Docker images using docker-compose
echo Building Docker images...
docker-compose build
if errorlevel 1 (
    echo ERROR: Docker Compose build failed.
    pause
    exit /b 1
)

REM Start Docker containers in detached mode
echo Starting Docker containers...
docker-compose up -d
if errorlevel 1 (
    echo ERROR: Docker Compose up failed.
    pause
    exit /b 1
)

REM Wait for 10 seconds to allow services to start
timeout /t 10

REM Open the default browser at the desired URL 
echo Opening browser at http://localhost:8081...
start http://localhost:8081/ImageUploadServlet

pause
