# Running yamcs in a container

A container is an isolated environment where an application can be executed.

An image is the "template" for creating that environment, where all dependencies are already downloaded and installed.

*Notes* 
- It's highly recommended to use Docker Desktop to manage your containers.
- If you are using VSCode, you can add the Docker extension to manage the images and containers. 
## Building 

To build the image, run the following command, replacing `your-image-name` with a suitable name, for example `yamcs-image`

```bash
docker build -t your-image-name .
```

The dot . tells docker to build the image using the current directory. This will download all dependencies and create the isolated environment. Keep in mind, this process will take some time, depending on the speed of your computer and your internet connection. Also keep in mind that the following container will take about 1GB of storage space.

## Running for the first time

To run the container for the first time, execute the following command, again replacing `your-container-name`:

```bash
docker run --name your-container-name -p 8090:8090 --mount source=yamcs-data,target=/yamcs-instance/target/yamcs/yamcs-data your-image-name 
```
* `-p 8090:8090` exposes the containers local port 8090 to the localhost's 8090 port.
* `--mount source=yamcs-data, target=/yamcs-instance/target/yamcs/yamcs-data` binds the container's `/yamcs-instance/target/yamcs/yamcs-data` folder to a docker volume called `yamcs-data`. This volume has data that is persistent after container death. Without it, yamcs' data would be deleted each time the container is stopped/rebuilt.

When you execute the `run` command for the first time, the container will be created and started. 
If everything goes well, yamcs will be automatically started and will be accessible at `localhost:8090`.

## Running and Stopping 

Executing `run` if the container is stopped will result in an error, because the container already exists. In order to initiate an already created container, use:

```bash
docker start your-container-name -i
```

*Notes* 
- You don't need the port argument here.
- In the above command, `-i` stands for interactive, so you can have access to what yamcs prints to the terminal. You can't execute any commands though. To do that, run:
```bash
docker exec -it your-container-name bash
```  
This opens a new bash terminal attached to the container.

To stop the container, simply: 

```bash
docker stop your-container-name
```

## Re-installing

In case you want to modify the dockerfile, you need to remove the already created container using the command: 

```bash
docker rm your-container-name
```