=========================
 Crate JMX HTTP Exporter
=========================

.. image:: https://travis-ci.org/crate/jmx_exporter.svg?branch=master
    :target: https://travis-ci.org/crate/jmx_exporter
    :alt: Travis CI

|
This project implements a `javaagent` which exposes CrateDB specific JMX MBean
metrics over HTTP.

Usage
=====

`Download the jar`_ and enable the agent using the `CRATE_JAVA_OPTS` environment
variable.

Example usage:

::

   export CRATE_JAVA_OPTS="-javaagent:<PATH_TO>/crate-jmx-exporter-0.2.0.jar=8080"
   ./bin/crate

Afterwards the `Available HTTP endpoints`_ are accessible via HTTP at
http://localhost:8080/<ENDPOINT>.

To bind the java agent to a specific IP change the port number to ``host:port``.

Available HTTP endpoints
------------------------

/metrics
~~~~~~~~

This endpoint will return general JVM and CrateDB specific JMX MBean metrics as
plain text. The text format is following the exporter format used by `Prometheus`_.

.. note::

   CrateDB JMX MBeans are only available as an `Enterprise Feature`_.
   See documentation about `JMX Monitoring`_ for details.

/ready
~~~~~~

This endpoint will return a HTTP status code only, no content is returned.
The status code indicates if the CrateDB node is able to process SQL
statements. This could not be the case e.g. when a node is gracefully shutdown.
The readiness endpoint can be usefull for load blancers to know when to remove a
node from the pool.

Status codes:

- 200 SQL processing is enabled.
- 503 SQL processing is disabled.
- 501 `ready` JMX metric is not available (e.g. enterprise edition is not
  enabled for the current node)

Contributing
============

This project is primarily maintained by `Crate.io`_, but we welcome community
contributions!

See the `developer docs`_ and the `contribution docs`_ for more information.

Help
====

Looking for more help?

- Check `StackOverflow`_ for common problems
- Chat with us on `Slack`_
- Get `paid support`_

.. _Download the jar: https://dl.bintray.com/crate/crate/io/crate/crate-jmx-exporter/0.2.1/crate-jmx-exporter-0.2.1.jar
.. _Enterprise Feature: https://crate.io/docs/crate/reference/en/latest/enterprise/index.html
.. _JMX Monitoring: https://crate.io/docs/crate/reference/en/latest/admin/monitoring.html
.. _Prometheus: https://prometheus.io/docs/instrumenting/writing_exporters/
.. _contribution docs: CONTRIBUTING.rst
.. _Crate.io: http://crate.io/
.. _CrateDB: https://github.com/crate/crate
.. _developer docs: DEVELOP.rst
.. _paid support: https://crate.io/pricing/
.. _Slack: https://crate.io/docs/support/slackin/
.. _StackOverflow: https://stackoverflow.com/tags/crate
