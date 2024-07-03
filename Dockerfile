# Use a base image with Java installed
FROM openjdk:17

# Install Android SDK (this might vary depending on your project requirements)
RUN apt-get update && \
    apt-get install -y wget unzip && \
    wget https://dl.google.com/android/repository/commandlinetools-linux-6609375_latest.zip -O /cmdline-tools.zip && \
    unzip /cmdline-tools.zip -d /sdk && \
    rm /cmdline-tools.zip && \
    mkdir -p /sdk/cmdline-tools && \
    mv /sdk/tools /sdk/cmdline-tools/tools && \
    yes | /sdk/cmdline-tools/tools/bin/sdkmanager --licenses && \
    /sdk/cmdline-tools/tools/bin/sdkmanager "platforms;android-30" "build-tools;30.0.3"

# Set environment variables
ENV ANDROID_HOME=/sdk
ENV PATH=$PATH:$ANDROID_HOME/cmdline-tools/tools/bin:$ANDROID_HOME/platform-tools

# Set the working directory
WORKDIR /app

# Copy the project files to the working directory
COPY . /app

# Build the project using Gradle
RUN ./gradlew build

# Define the command to run the application
CMD ["./gradlew", "assembleDebug"]
