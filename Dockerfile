# Base image
FROM ubuntu:latest

# Update packages and install necessary dependencies
RUN apt-get update && \
    apt-get install -y \
    software-properties-common \
    python3-pip \
    openjdk-11-jdk \
    maven

# Set environment variables for Java and Maven
ENV JAVA_HOME /usr/lib/jvm/java-11-openjdk-amd64
ENV PATH $PATH:$JAVA_HOME/bin
ENV MAVEN_HOME /usr/share/maven

# Install Python packages
RUN pip3 install --upgrade pip

# Set the application's working directory
WORKDIR /yamcs-instance

# Copy files from the current folder to the container 
COPY . . 

# Download the dependencies specified in the pom.xml file
RUN mvn dependency:go-offline

# Install the requirements for the communication scripts
RUN pip install -r /yamcs-instance/communication/requirements.txt


# Expose the internal port yamcs port
EXPOSE 8090

# Define a volume for persistent storage
VOLUME /yamcs-data


# Define the command to run your application
CMD ["mvn", "yamcs:run"]
