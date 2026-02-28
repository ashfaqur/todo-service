## Objective - Todo Service

Design and implement a backend service allowing basic management of a simple to-do list.
 
# Functional requirements

Each to-do item has the following attributes:
- description
- status: "not done", "done", "past due"
- date-time of creation
- due date-time
- date-time at which the item was marked as done

The service should provide a RESTful API that allows to:
- add an item
- change description of an item
- mark an item as "done"
- mark an item as "not done"
- get all items that are "not done" (with an option to retrieve all items)
- get details of a specific item
  
The service should automatically change status of items that are past their due date as "past due".

The service should forbid changing "past due" items via its REST API.

## Non-functional requirements

the service should:
- be dockerized
- use H2 in-memory database
- contain automatic tests (we don't expect a very high coverage but would like to see your approach to writing automatic tests)

the service should not:
- implement authentication

readme should contain brief notes covering:
- service description and made assumptions
- tech stack used (runtime environment, frameworks, key libraries)
- how to:
  - build the service
  - run automatic tests
  - run the service locally

## Evaluation criteria

- alignment with the requirements
- usage of best practices when dealing with edge cases that are not covered here
- code quality and readability
- presence and quality of (or lack of) automatic tests
- commit history (thought process, commit messages)

Your work should be handed off in a form of a link to a git repository we can clone into.
