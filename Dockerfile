FROM openjdk:17-jdk-slim

WORKDIR /app

# Install dependencies (Python, Maven)
RUN apt-get update && apt-get install -y python3 python3-pip maven curl

# Copy project files
COPY pom.xml /app/pom.xml
COPY src /app/src
COPY frontend /app/frontend
COPY app.py /app/app.py
COPY requirements.txt /app/requirements.txt

# Install Python dependencies
RUN pip3 install --no-cache-dir -r requirements.txt

# Build Java project with Maven
RUN mvn clean package

EXPOSE 8000

CMD ["uvicorn", "app:app", "--host", "0.0.0.0", "--port", "8000"]
