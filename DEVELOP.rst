===============
Developer Guide
===============

Building
========

This project uses Gradle_ as build tool.

Gradle can be invoked like so::

  $ ./gradlew

The first time this command is executed, Gradle is downloaded and bootstrapped
for you automatically.

Building the JAR
================

Run::

  $ ./gradlew clean jar

Testing
=======

Run the unit and integration tests like so::

  $ ./gradlew test

By default, integration tests run against the latest CrateDB version. To test against a specific CrateDB version::

  $ ./gradlew test -P crateDbVersion=X.Y.Z

Note::

  If running tests on ARM Mac, there may not be a crate release for aarch64_mac available. The tests will fall back to
  a x64_mac release. In this case, you need Rosetta 2 installed for the tests to work.

.. _Gradle: https://gradle.org/


Release
=======

Preparing a release
-------------------
- Update ``project.version`` with the version to release in ``build.gradle``

- Add a section for the new version in the ``CHANGES.txt`` file

- Commit your changes with a message like "prepare release x.y.z"

- Push changes to origin

- Create a tag by running ``git tag <x.y.z>``

- Push tag to origin by running ``git push --tags``

Deploy to Maven Central
-----------------------
The artifacts can be uploaded to maven central using ``./gradlew publishToSonatype closeAndReleaseSonatypeStagingRepository``.
This gradle task requires signing and sonatype credentials. Alternatively, you can run the
[jenkins job](https://jenkins.crate.io/job/jmx_exporter/job/release), and provide the newly
created tag version.

After the release
-----------------
- Update ``README.rst`` to point to the newly uploaded jar

- Commit your changes with a message like "reflect release x.y.z"

- Push changes to origin

- Create new release in: https://github.com/crate/jmx_exporter/releases. Use
  [1.2.2](https://github.com/crate/jmx_exporter/releases/tag/1.2.2) as an example.

