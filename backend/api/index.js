import express from 'express';
import cors from 'cors';
import dotenv from 'dotenv';
import appRouter from '../src/routes/appRouter.js';

dotenv.config();

const app = express();

// Security: Disable X-Powered-By
app.disable('x-powered-by');

// Middleware: CORS
const allowedOrigins = (process.env.ALLOWED_ORIGINS || 'https://freefireofficial.in,https://gamervoult.vercel.app')
  .split(',')
  .map(o => o.trim())
  .filter(Boolean);

app.use(cors({
  origin: (origin, callback) => {
    if (!origin || allowedOrigins.includes(origin) || origin.endsWith('.vercel.app') || process.env.NODE_ENV !== 'production') {
      return callback(null, true);
    }
    return callback(new Error('CORS origin rejected'));
  },
  credentials: true,
  methods: ['GET', 'POST', 'PUT', 'DELETE', 'OPTIONS'],
  allowedHeaders: ['Content-Type', 'Authorization', 'x-webhook-secret', 'x-api-key']
}));

// Middleware: JSON Body Parser with reasonable limits
app.use(express.json({ limit: '1mb' }));
app.use(express.urlencoded({ extended: true, limit: '1mb' }));

// Middleware: Safe Redacting Request Logger
app.use((req, res, next) => {
  const start = Date.now();
  res.on('finish', () => {
    const duration = Date.now() - start;
    if (req.path !== '/api/health') {
      console.log(`[${new Date().toISOString()}] ${req.method} ${req.path} -> ${res.statusCode} (${duration}ms)`);
    }
  });
  next();
});

// Mount Gamervoult Application Routes
app.use(appRouter);

// 404 Catch-All
app.use((req, res) => {
  return res.status(404).json({
    success: false,
    error: 'NOT_FOUND',
    message: `API endpoint ${req.method} ${req.path} not found.`
  });
});

// Safe Global Error Handler (Never leak stack traces)
app.use((err, req, res, next) => {
  console.error('[GlobalError]', err.message);
  return res.status(err.status || 500).json({
    success: false,
    error: 'INTERNAL_SERVER_ERROR',
    message: process.env.NODE_ENV === 'production' ? 'An internal error occurred.' : err.message
  });
});

// Start server if executed directly (Local development)
const PORT = process.env.PORT || 5000;
if (process.env.NODE_ENV !== 'test' && !process.env.VERCEL) {
  app.listen(PORT, () => {
    console.log(`🚀 Gamervoult Backend listening on http://localhost:${PORT}`);
  });
}

export default app;
