ToDo list (24.05.2014 -> 27.05.2014):

- report
  - FindBugs background, 
  - introduction & background (visitor pattern, BCEL library)
  - Solution approach, problem statement, related work
- implementation:
  - find and adapt a project from Head First Design Patterns and use as the show case. 
  - add a dummy checker
    a. give an alert if a method (m) is being called.
    b. give an alert if m1 is called after m2.

- NP_TOSTRING_COULD_RETURN_NULL && NP_CLONE_COULD_RETURN_NULL --> FindNullDeref.java
  - a new bug type can be also added here

- build
  - ant build findbugs-sc
  - FindBugs.xml & Messages.xml will change
