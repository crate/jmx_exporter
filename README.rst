=========================
 Crate JMX HTTP Exporter
=========================

This project implements a `javaagent` which exposes CrateDB specific JMX MBean
metrics over HTTP.

Usage
=====

`Download the jar`_ and enable the agent using the `CRATE_JAVA_OPTS` environment
variable.

Example usage:

::

   export CRATE_JAVA_OPTS="-javaagent:<PATH_TO>/crate-jmx-exporter-1.0.0.jar=8080"
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
See documentation about `JMX Monitoring`_ for details.

.. note::

   Monitoring CrateDB JMX MBeans prior CrateDB 4.5 requires an `Enterprise License`_.


/ready
~~~~~~

This endpoint will return a HTTP status. The status code indicates whether the
*individual* node is ready to accept statements.

If statements can be processed successfully also depends on the cluster state,
but this endpoint *doesn't* take the cluster state into consideration.

The main use-case for this endpoint is to use it with load-balancers. If a node
is gracefully stopped, this endpoint will start returning ``503``, allowing the
load-balancer to remove the node.

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

- Check out our `support channels`_

.. _contribution docs: CONTRIBUTING.rst
.. _Crate.io: http://crate.io/
.. _CrateDB: https://github.com/crate/crate
.. _developer docs: DEVELOP.rst
.. _Download the jar: https://repo1.maven.org/maven2/io/crate/crate-jmx-exporter/
.. _Enterprise License: https://crate.io/docs/crate/reference/en/4.4/editions.html#enterprise-features
.. _JMX Monitoring: https://crate.io/docs/crate/reference/en/latest/admin/monitoring.html
.. _Prometheus: https://prometheus.io/docs/instrumenting/writing_exporters/
.. _support channels: https://crate.io/support/
