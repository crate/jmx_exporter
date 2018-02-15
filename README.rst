=========================
 Crate JMX HTTP Exporter
=========================

This project implements a `javaagent` which exposes CrateDB specific metrics over
HTTP.

.. warning::

   THIS PROJECT IS CURRENTLY IN EARLY ALPHA STATE!

Usage
=====

::

   export CRATE_JAVA_OPTS="-javaagent:<PATH_TO>/crate_jmx_exporter-0.1.0.jar=<HOST>:<PORT>"
   ./bin/crate

Afterwards the metrics can be fetched via HTTP under the configured host and port.

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

.. _contribution docs: CONTRIBUTING.rst
.. _Crate.io: http://crate.io/
.. _CrateDB: https://github.com/crate/crate
.. _developer docs: DEVELOP.rst
.. _paid support: https://crate.io/pricing/
.. _Slack: https://crate.io/docs/support/slackin/
.. _StackOverflow: https://stackoverflow.com/tags/crate
