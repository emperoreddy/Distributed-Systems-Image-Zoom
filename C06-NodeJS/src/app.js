const express = require("express");
const bodyParser = require("body-parser");
const { MongoClient, ObjectId } = require("mongodb");
const punycode = require("punycode/");


const app = express();
const PORT = process.env.PORT || 3000;

// MongoDB configuration
const MONGO_URI = "mongodb://localhost:27017";
const DB_NAME = "zoomServiceDB";
let db;

// Middleware
app.use(bodyParser.json());

// Connect to MongoDB
MongoClient.connect(MONGO_URI, {
  useNewUrlParser: true,
  useUnifiedTopology: true,
})
  .then((client) => {
    db = client.db(DB_NAME);
    console.log("Connected to MongoDB");
  })
  .catch((err) => {
    console.error("Failed to connect to MongoDB", err);
    process.exit(1);
  });

// Routes

// Upload BMP image
app.post("/api/bmp/upload", async (req, res) => {
  try {
    const { imageData, zoomLevel } = req.body;
    const result = await db
      .collection("bmp_images")
      .insertOne({ imageData, zoomLevel });
    res
      .status(201)
      .json({ message: "BMP uploaded successfully", id: result.insertedId });
  } catch (error) {
    res.status(500).json({ error: error.message });
  }
});

// Get BMP image by ID
app.get("/api/bmp/:id", async (req, res) => {
  try {
    const { id } = req.params;
    const bmp = await db
      .collection("bmp_images")
      .findOne({ _id: new ObjectId(id) });
    if (!bmp) {
      return res.status(404).json({ error: "BMP not found" });
    }
    res.contentType("image/bmp");
    res.send(bmp.imageData);
  } catch (error) {
    res.status(500).json({ error: error.message });
  }
});

// Get SNMP values
app.get("/api/snmp/values", async (req, res) => {
  try {
    const snmpValues = await db.collection("snmp_values").find().toArray();
    res.status(200).json(snmpValues);
  } catch (error) {
    res.status(500).json({ error: error.message });
  }
});

// Start the server
app.listen(PORT, () => {
  console.log(`Server is running on http://localhost:${PORT}`);
});
