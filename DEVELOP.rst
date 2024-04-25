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

.. _Gradle: https://gradle.org/


Preparing a Release
===================

To create a new release, you must:

- Update ``project.version`` with the version to release in ``build.gradle``

- Add a section for the new version in the ``CHANGES.txt`` file

- Commit your changes with a message like "prepare release x.y.z"

- Push changes to origin

- Create a tag by running ``git tag <x.y.z>``

- Push tag to origin by running ``git push --tags``

- Deploy to maven central (see section below)

- Update ``README.rst`` to point to the newly uploaded jar

- Commit your changes with a message like "reflect release x.y.z"

- Push changes to origin


Maven Central Deployment
========================

The artifacts can be uploaded to maven central using ``./gradlew uploadArchives closeAndReleaseRepository``.
This gradle task requires signing and sonatype credentials.
