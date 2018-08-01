=====================================
 Changes for Crate JMX HTTP Exporter
=====================================

Unreleased
==========

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

