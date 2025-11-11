=====================================
 Changes for Crate JMX HTTP Exporter
=====================================

Unreleased
==========

- Changed the value of ``roles`` introduced with ``1.2.2`` to contain
  all roles of the node in one metric, and for each role to have the
  value ``true`` instead of repeating the name of the role. These
  changes facilitate easier queries in Grafana, to determine which
  nodes have certain roles. e.g.:

      ``crate_roles{is_master_eligible=\"is_master_eligible\",}``
      ``crate_roles{is_data=\"is_data\",}``

  changed to:

      ``crate_roles{is_data=\"true\",is_master_eligible=\"true\",}``

- Changed values of flag ``primary``, for shard info, introduced with
  ``1.2.2``, to lower case ``true``, ``false``, and empty string if
  the attribute is missing.

- Changed behavior of ``schema`` property of ``shard_info`` to always be
  included, with value of empty string, even for CrateDB versions < ``5.8.2``
  which didn't expose it.
  
- Fixed an issue introduced with ``1.2.2`` that would cause an exception
  to be thrown, leading to metrics being unavailable at the ``/`` and
  ``/metrics`` endpoint.

2025/11/04 1.2.2
================

- Added a ``is_master`` flag to ``crate_node`` metrics. e.g.:

    ``crate_node{is_master=0.0}``
    ``crate_node{is_master=1.0}``

- Added flags to ``crate_node`` metrics which show the role of the node.
  Each one of those are included in the metrics with value ``1.0`` if the
  node has the corresponding role. If the node doesn't have a role, the role
  is missing from the metrics. e.g.:

    ``crate_node{is_master_eligible=1.0}``
    ``crate_node{is_data=1.0}``
    ``crate_node{is_master_eligible=1.0, is_data=1.0}``

- Added a ``primary`` flag to the ``crate_node{name="shard_info"}`` metrics.

2025/09/18 1.2.1
================

- Fixed an issue that caused both ``Content-Length`` and ``Transfer-Encoding:
  chunked`` headers to be set for responses on the ``/metrics`` endpoint.
  This could cause issues if using the jmx-exporter behind a NGINX proxy or
  other strict clients.

- Fixed an issue that caused the ``/ready`` endpoint to respond slowly if the
  CrateDB node holds a lot of tables/shards.


2024/08/20 1.2.0
================

- Added a ``schema`` property to the ``crate_node{name="shard_info"}`` metrics.

2024/04/25 1.1.0
================

- Added support for more detailed connection metrics:
    * ``HttpMessagesReceived``
    * ``HttpBytesReceived``
    * ``HttpMessagesSent``
    * ``HttpBytesSent``
    * ``PsqlMessagesReceived``
    * ``PsqlBytesReceived``
    * ``PsqlMessagesSent``
    * ``PsqlBytesSent``
    * ``TransportTotal``
    * ``TransportMessagesReceived``
    * ``TransportBytesReceived``
    * ``TransportMessagesSent``
    * ``TransportBytesSent``

- Migrated from Bintray to Maven Central. All new releases go to Maven Central
  from now on.

2021/01/28 1.0.0
================

- Made sure unsupported jmx datatypes are only logged and not cause to stop the
  agent.

- Added new shard related metrics to ``NodeInfo`` reporting detailed information
  about the shards located on the node.

Breaking changes
----------------

- Dropped support for java <11

2019/02/18 0.6.0
================

- Added a new ``QueryStats`` metric reporting the number of statements that
  did not complete successfully.

2019/02/15 0.5.0
================

- Added new ``QueryStats`` metrics reporting the total number of statements and
  the sum of statements execution durations grouped by statement type.

2018/09/12 0.4.0
================

- Don't create log warning entries for ``crate_node_info`` attributes that are
  ignored by intend.

- Added a new metric under ``/metrics`` endpoint which exposes statistics of
  each circuit breaker.

2018/08/03 0.3.0
================

- Removed the ``crate_node_info`` metric previously exposed under the
  ``/metrics`` endpoint.

- Added a new metric under the ``/metrics`` endpoint which exposes the cluster
  state version.

2018/07/24 0.2.1
================

- Added a new metric under ``/metrics`` endpoint which exposes statistics of
  each thread pool.

- Added a new metric under ``/metrics`` endpoint which exposes the number of
  open and total connections per protocol.

2018/04/16 0.2.0
================

- Added a new "metric" under /metrics endpoint which exposes NodeInfoMBean
  attributes (NodeId, NodeName).

2018/03/01 0.1.1
================

- Fixed an issue which resulted in duplicate crate metrics output when
  requesting the metrics multiple times.

