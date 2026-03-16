const http = require('http');
const fs = require('fs');
const path = require('path');

const PORT = 18790;
const UPLOAD_DIR = 'C:\\Users\\Administrator\\.openclaw\\media\\inbound';

const server = http.createServer((req, res) => {
  if (req.method === 'POST' && req.url === '/upload') {
    let body = [];
    req.on('data', chunk => body.push(chunk));
    req.on('end', () => {
      // 简单解析multipart/form-data
      const boundary = req.headers['content-type'].split('boundary=')[1];
      if (!boundary) {
        res.writeHead(400, { 'Content-Type': 'text/plain' });
        res.end('No boundary found');
        return;
      }
      
      // 提取文件名和内容
      const bodyStr = Buffer.concat(body).toString();
      const filenameMatch = bodyStr.match(/filename="([^"]+)"/);
      const filename = filenameMatch ? filenameMatch[1] : 'unknown_file';
      
      // 提取文件内容 (简化处理)
      const fileContentMatch = bodyStr.split(boundary)[1];
      const fileStart = fileContentMatch.indexOf('\r\n\r\n') + 4;
      const fileEnd = fileContentMatch.lastIndexOf('\r\n--');
      const fileContent = fileContentMatch.substring(fileStart, fileEnd);
      
      const filePath = path.join(UPLOAD_DIR, filename);
      fs.writeFileSync(filePath, Buffer.from(fileContent));
      
      console.log(`File uploaded: ${filename}`);
      res.writeHead(200, { 'Content-Type': 'application/json' });
      res.end(JSON.stringify({ success: true, filename }));
    });
  } else if (req.method === 'GET' && req.url === '/status') {
    res.writeHead(200, { 'Content-Type': 'text/plain' });
    res.end('File upload server running');
  } else {
    res.writeHead(404);
    res.end();
  }
});

server.listen(PORT, '0.0.0.0', () => {
  console.log(`File upload server running on port ${PORT}`);
});
