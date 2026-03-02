# Development

In real production development, the commit usually invoves:
- Writing in development branch.
- When ready for review, squash or polish commits.
- Open a pull request (PR) to the main branch.
- Polish the PR description.
- Pass CI.
- Pass Review.
- Merge to main.

In this project for simplicity, the commits are directly
added to the main branch, the commit history is thus unpolished,
and hard to follow the development process.

In order give an overview of the development process, here is
an high level overview of the milestones.

## Milestones

1. Carefully read thorugh and understand the project requirements.
2. Design the API and DB schema for document it
3. Deep dive in understanding and handling of edge cases.
4. Setup up the initial project scaffolding with docker setup.
5. Implementation of the POST create and GET single todo task.
   This helps to test end to end of the project lifecycle.
6. Implementation of the GET all todo API endpoint.
   In this phase implement the update mechanism for PAST_DUE and its immutability rules
7. Implementation of PATCH update description, POST mark done and not-done.
   Also handle rule for mark done being terminal state.
