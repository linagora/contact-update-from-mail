## API to store predictions and exclusions' lists

This API stores the predictions and the exclusions' lists for the tool ContactUpdateFromMail.

### Prerequisites

You will need NodeJs, NPM and MongoDb installed to run this API.

### Installation
Download the folder.
Launch your mongoDB server using the following command in a terminal
```
mongod
```
Open another terminal, go to the root of this folder and try 
```
node index
```
This should start the API on localhost:8080. 

You can open your browser on [http://localhost:8080/](http://localhost:8080/) to check that it works.

### Usage
This API supports the following methods for the predictions :
* GET to api/contacts/predictionList/:dest
* GET to api/contacts/predictionList/:dest/:exp
* PUT to api/contacts/predictionList/:dest/:exp
* DELETE to api/contacts/predictionList/:dest/:exp
And these under for the exclusions :
* GET to api/contacts/exclusionList/:dest/:exp
* PUT to api/contacts/exclusionList/:dest/:exp
* DELETE to api/contacts/exclusionList/:dest/:exp

### Testing
Tests have been realised using [Postman](https://www.postman.com/).
