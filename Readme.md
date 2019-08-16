# ACS Interface for External Collaboration


## Project Objective
This project is a POC to demonstrate how **specific** ACS content can be access by external users.

The interface will allow users to interact with a single folder path. Create, copy, move, delete and search operations are restricted to this predefined folder structure, so content outside this folder structure is not visible to the application.

## Disclaimer
The tool and code used in this project is not officially supported by Alfresco. It is used as a POC to demonstrate how to work with a CMIS repository.

## Building the jar artifact
The jar artifact it is already available in the target folder (target/acs-external-collaboration-x.x.x.jar), together with the application.properties file to be used for deployment.

To rebuild the artifact download the project and run the following command from the root directory.

>mvn package -Dmaven.test.skip=true

This command will compile and create a new jar artifact in the target folder.

## Running the application
To run the application first edit the application.properties file and set the following properties pointing to your ACS repository.


>alfresco_atompub_url=http://127.0.0.1:8080/alfresco/api/-default-/public/cmis/versions/1.1/atom
>repository_id=-default-
>root_folder=/External Collaboration 

Create the root folder in your ACS repository (/External Collaboration).

Set the relevant permissions on this folder to allow external users to collaborate.

By detault this application is running on port 8888 but this can be changed in the application.properties file

>server.port=8888

The application can be accessed via browser on http://host:8888, replacing host with the hostname/IP address of the server running the application.

You will need a valid username and password in ACS to login to the application.

