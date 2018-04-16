=====================================
 Changes for Crate JMX HTTP Exporter
=====================================

Unreleased
==========

- Added a new "metric" under /metrics endpoint which exposes NodeInfoMBean
  attributes (NodeId, NodeName).

2018/03/01 0.1.1
================

- Fixed an issue which resulted in duplicate crate metrics output when
  requesting the metrics multiple times.

