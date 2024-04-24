# Contributing

This project could use a support and contributors are very welcomed. If you feel that something has to be
changed or a bug to be fixed, you can report a [new issue](https://github.com/CodexioLtd/spring-requery/issues/new), and
we can take care of it.

If you want to submit directly a code fix, we will be more than glad to see it. Fork the repository and start a clean
branch out of the version you want to patch. When you are finished, make sure all your tests are passing and the
coverage remains in decent level by executing `mvn clean test jacoco:report -Pmvn-deploy`.

Please use the [code style](./codestyle.xml)
in the project root folder. If your IDE does not support it, we strongly encourage you just to follow
the code styling in the rest of the classes and methods.

After all, your tests are passing and the coverage seems good to you, create a
[pull request](https://github.com/CodexioLtd/spring-requery/compare). We will review the request and either leave
some meaningful suggestions back or maybe merge it and release it with the next release.
