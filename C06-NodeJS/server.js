
const express = require("express");
const mongoose = require("mongoose");
const mysql = require("mysql2/promise");
const cors = require("cors");


const PORT = process.env.PORT || 3000;

// MongoDB connection 
const MONGO_HOST = process.env.MONGO_HOST || "c06-mongo";
const MONGO_PORT = process.env.MONGO_PORT || "27017";
const MONGO_DB = process.env.MONGO_DB || "snmpdb";

// MySQL connection 
const MYSQL_HOST = process.env.MYSQL_HOST || "c06-mysql";
const MYSQL_PORT = process.env.MYSQL_PORT || "3306";
const MYSQL_USER = process.env.MYSQL_USER || "root";
const MYSQL_PASSWORD = process.env.MYSQL_PASSWORD || "root";
const MYSQL_DATABASE = process.env.MYSQL_DATABASE || "imagesdb";


const app = express();

// allow requests from frontend
app.use(cors({
    origin: 'http://localhost:8081', 
    methods: ['GET', 'POST'],
    allowedHeaders: ['Content-Type', 'X-Unique-ID']
}));



app.use(express.raw({ type: "application/octet-stream", limit: "100mb" }));


app.use(express.json());


const mongoUri = `mongodb://${MONGO_HOST}:${MONGO_PORT}/${MONGO_DB}`;
mongoose
  .connect(mongoUri, {
    useNewUrlParser: true,
    useUnifiedTopology: true,
  })
  .then(() => console.log(`Connected to MongoDB at ${mongoUri}`))
  .catch((err) => {
    console.error("MongoDB connection error:", err);
    process.exit(1);
  });


const snmpSchema = new mongoose.Schema({
  hostname: { type: String, required: true },
  osName: { type: String, required: true },
  cpuUsage: { type: Number, required: true },
  ramUsage: { type: Number, required: true },
  timestamp: { type: Date, default: Date.now },
});


const SnmpModel = mongoose.model("SnmpModel", snmpSchema);


let mysqlPool;

async function initMySQL() {
  mysqlPool = await mysql.createPool({
    host: MYSQL_HOST,
    port: MYSQL_PORT,
    user: MYSQL_USER,
    password: MYSQL_PASSWORD,
    database: MYSQL_DATABASE,
    waitForConnections: true,
    connectionLimit: 10,
    queueLimit: 0,
  });

  console.log(`Connected to MySQL at ${MYSQL_HOST}:${MYSQL_PORT}`);


  const createTableQuery = `
    CREATE TABLE IF NOT EXISTS pictures (
      id INT AUTO_INCREMENT PRIMARY KEY,
      filename VARCHAR(255),
      mime_type VARCHAR(50),
      image_data LONGBLOB,
      created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
    )
  `;
  await mysqlPool.query(createTableQuery);
  console.log("Ensured MySQL table 'pictures' exists.");
}


initMySQL().catch((err) => {
  console.error("MySQL initialization error:", err);
  process.exit(1);
});


app.get("/api/snmp", async (req, res) => {
  try {
    const snmpData = await SnmpModel.find().sort({ timestamp: -1 }).exec();
    return res.json(snmpData);
  } catch (error) {
    console.error("Error fetching SNMP data:", error);
    return res.status(500).json({ error: "Internal Server Error" });
  }
});


app.post("/api/bmp/upload", async (req, res) => {
  try {
    if (!req.body || !req.body.length) {
      return res.status(400).json({ error: "No image data provided." });
    }

   
    const filename = `uploaded_${Date.now()}.bmp`;
    const mimeType = "image/bmp"; 

    const [result] = await mysqlPool.query(
      "INSERT INTO pictures (filename, mime_type, image_data) VALUES (?, ?, ?)",
      [filename, mimeType, req.body]
    );

    return res.status(201).json({
      message: "Image uploaded successfully",
      pictureId: result.insertId,
    });
  } catch (error) {
    console.error("Error storing BMP in MySQL:", error);
    return res.status(500).json({ error: "Internal Server Error" });
  }
});


app.get("/api/bmp/:id", async (req, res) => {
  try {
    const pictureId = parseInt(req.params.id, 10);
    if (isNaN(pictureId)) {
      return res.status(400).json({ error: "Invalid picture ID" });
    }

    const [rows] = await mysqlPool.query(
      "SELECT * FROM pictures WHERE id = ?",
      [pictureId]
    );

    if (!rows || rows.length === 0) {
      return res.status(404).json({ error: "Picture not found" });
    }

    const picture = rows[0];
    res.setHeader("Content-Type", picture.mime_type);
    return res.send(picture.image_data);
  } catch (error) {
    console.error("Error fetching BMP from MySQL:", error);
    return res.status(500).json({ error: "Internal Server Error" });
  }
});





app.listen(PORT, () => {
  console.log(`Container 6 (Node.js) running on port ${PORT}`);
});
