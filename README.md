# xsl-gdal-transform
A saxon extension function for transforming xy's from one spatial reference to another

<p>This class implements an xsl function that can be used to convert
xy coordinates from one spatial reference to another via a call to
gdaltransform. The form of the xsl function is:</p>

<p><code>srxform:srtransform([coord-string-x], [coord-string-y], [source-sr], [target-sr])</code></p>

  <ul><li>coord-string-x: The source X coordinate</li>
  <li>coord-string-y: The source Y coordinate</li>
  <li>source-sr: The spatial reference of the source coordinates as required by the
  -s_srs switch of gdaltransform.</li>
  <li>target-sr: The spatial reference of the output coordinates as required by the
  -t_srs switch of gdaltransform.</li></ul>

<p><code>srxform</code> is a namespace name pointing to the namespace <code>java:or.portland.SRTransform</code></p>

<p>Coordinates passed in, although passed in as strings, must be integers
or decimals.</p>

<p>This function relies on the GDAL library. Specifically, GDAL must be
present on the server, and an environment variable GDAL_APPS must be set
that points to the directory where the gdaltransform executable can be
found. If the environment variable is not set or can't be found in the
GDAL_APPS directory, the xsl function will not fail, but it also will not
perform the transformation.</p>

<p>This function will also not fail when passed bad data. If it is passed
coordinate values that are not parseable as a number, it will return
a 0 for that coordinate. If any exceptions are thrown while transforming
the coordinates, both coordinates will be returned as zeroes.
