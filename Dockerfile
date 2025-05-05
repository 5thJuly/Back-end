# Sử dụng hình ảnh JDK 17 làm base image
FROM openjdk:21-jdk-slim

# Đặt thông tin tác giả
LABEL maintainer="Admin"

# Đặt thư mục làm việc trong container
WORKDIR /app

COPY target/technihongo.jar /app/technihongo.jar

# Expose cổng mà ứng dụng sẽ chạy
EXPOSE 3000

ENTRYPOINT ["java", "-jar", "/app/technihongo.jar"]
