import React, { useState, useEffect, useRef } from 'react';
import { 
  FileText, Upload, Send, RefreshCw, Cpu, HelpCircle, 
  CheckCircle2, AlertCircle, Bot, User, Sparkles, Key, 
  X, Layers, MessageSquare, ChevronDown, ChevronUp 
} from 'lucide-react';

const API_BASE = '';

export default function App() {
  // State variables
  const [file, setFile] = useState(null);
  const [uploading, setUploading] = useState(false);
  const [apiKey, setApiKey] = useState(localStorage.getItem('gemini_api_key') || '');
  const [question, setQuestion] = useState('');
  const [asking, setAsking] = useState(false);
  const [error, setError] = useState(null);
  const [success, setSuccess] = useState(null);
  
  // Dashboard statistics state
  const [stats, setStats] = useState({
    pdfUploaded: false,
    fileName: null,
    questionsAsked: 0,
    chunksCreated: 0,
    aiModel: 'Google Gemini 1.5 Flash',
    recentQuestions: []
  });

  // Chat message history
  const [messages, setMessages] = useState([
    {
      sender: 'ai',
      text: 'Hello! I am your AI PDF Assistant. Please drag & drop or select a PDF file above to get started.',
      timestamp: new Date().toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })
    }
  ]);

  const [dragActive, setDragActive] = useState(false);
  const [expandedContext, setExpandedContext] = useState({});
  const messagesEndRef = useRef(null);
  const fileInputRef = useRef(null);

  // Auto-scroll chat to bottom
  const scrollToBottom = () => {
    messagesEndRef.current?.scrollIntoView({ behavior: 'smooth' });
  };

  useEffect(() => {
    scrollToBottom();
  }, [messages, asking]);

  // Save API key to localStorage when changed
  const handleApiKeyChange = (val) => {
    setApiKey(val);
    localStorage.setItem('gemini_api_key', val);
  };

  // Fetch initial stats
  const fetchStats = async () => {
    try {
      const res = await fetch(`${API_BASE}/api/pdf/stats`);
      if (res.ok) {
        const data = await res.json();
        setStats(data);
      }
    } catch (err) {
      console.error('Failed to fetch stats:', err);
    }
  };

  useEffect(() => {
    fetchStats();
  }, []);

  // Drag and drop handlers
  const handleDrag = (e) => {
    e.preventDefault();
    e.stopPropagation();
    if (e.type === 'dragenter' || e.type === 'dragover') {
      setDragActive(true);
    } else if (e.type === 'dragleave') {
      setDragActive(false);
    }
  };

  const handleDrop = (e) => {
    e.preventDefault();
    e.stopPropagation();
    setDragActive(false);
    if (e.dataTransfer.files && e.dataTransfer.files[0]) {
      handleUpload(e.dataTransfer.files[0]);
    }
  };

  const handleFileChange = (e) => {
    if (e.target.files && e.target.files[0]) {
      handleUpload(e.target.files[0]);
    }
  };

  // Upload PDF handler
  const handleUpload = async (selectedFile) => {
    if (!selectedFile.name.toLowerCase().endsWith('.pdf')) {
      setError('Please select a valid PDF document (.pdf)');
      return;
    }

    setError(null);
    setSuccess(null);
    setUploading(true);

    const formData = new FormData();
    formData.append('file', selectedFile);

    try {
      const response = await fetch(`${API_BASE}/api/pdf/upload`, {
        method: 'POST',
        body: formData,
      });

      const data = await response.json();

      if (!response.ok || !data.success) {
        throw new Error(data.error || 'Failed to process PDF.');
      }

      setFile(selectedFile);
      setStats(data.stats);
      setSuccess(`Successfully extracted & embedded ${data.chunksCreated} text chunks from "${selectedFile.name}"!`);
      
      // System message in chat
      setMessages(prev => [
        ...prev,
        {
          sender: 'ai',
          text: `📄 **"${selectedFile.name}"** uploaded successfully! Generated **${data.chunksCreated} text chunks** using in-memory vector storage. You can now ask any question related to this PDF.`,
          timestamp: new Date().toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })
        }
      ]);
    } catch (err) {
      setError(err.message || 'Error uploading file. Please ensure backend server is running on port 8080.');
    } finally {
      setUploading(false);
    }
  };

  // Ask question handler
  const handleAsk = async (e) => {
    if (e) e.preventDefault();
    const query = question.trim();
    if (!query) return;

    if (!stats.pdfUploaded) {
      setError('Please upload a PDF document before asking questions.');
      return;
    }

    setError(null);
    setQuestion('');
    
    const userMsg = {
      sender: 'user',
      text: query,
      timestamp: new Date().toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })
    };
    
    setMessages(prev => [...prev, userMsg]);
    setAsking(true);

    try {
      const response = await fetch(`${API_BASE}/api/pdf/ask`, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          'X-Gemini-Api-Key': apiKey
        },
        body: JSON.stringify({ question: query, apiKey: apiKey })
      });

      const data = await response.json();

      if (!response.ok || data.error) {
        throw new Error(data.error || 'Failed to retrieve answer from Gemini.');
      }

      setStats(data.stats);

      const aiMsg = {
        sender: 'ai',
        text: data.answer,
        retrievedContext: data.retrievedContext,
        timestamp: new Date().toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })
      };

      setMessages(prev => [...prev, aiMsg]);
    } catch (err) {
      setError(err.message || 'Failed to get answer from AI. Verify API key and backend status.');
      setMessages(prev => [
        ...prev,
        {
          sender: 'ai',
          text: `⚠️ **Error:** ${err.message || 'Unable to generate response.'}`,
          timestamp: new Date().toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })
        }
      ]);
    } finally {
      setAsking(false);
    }
  };

  // Reset session handler
  const handleReset = async () => {
    try {
      await fetch(`${API_BASE}/api/pdf/reset`, { method: 'POST' });
      setFile(null);
      setSuccess(null);
      setError(null);
      fetchStats();
      setMessages([
        {
          sender: 'ai',
          text: 'Session reset. Upload a new PDF file to begin.',
          timestamp: new Date().toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })
        }
      ]);
    } catch (err) {
      console.error(err);
    }
  };

  const toggleContext = (idx) => {
    setExpandedContext(prev => ({ ...prev, [idx]: !prev[idx] }));
  };

  return (
    <div className="app-container">
      {/* Top Navbar */}
      <nav className="navbar">
        <div className="navbar-content">
          <div className="logo-brand">
            <div className="logo-icon">
              <FileText size={22} />
            </div>
            <div>
              <span className="brand-title">AI PDF Chat Assistant</span>
              <span className="brand-badge" style={{ marginLeft: '8px' }}>RAG Powered</span>
            </div>
          </div>

          <div className="api-key-control">
            <Key size={16} color="#64748b" />
            <input 
              type="password"
              className="api-key-input"
              placeholder="Gemini API Key (Optional)..."
              value={apiKey}
              onChange={(e) => handleApiKeyChange(e.target.value)}
              title="Enter Gemini API key if not set in GEMINI_API_KEY environment variable"
            />
          </div>
        </div>
      </nav>

      {/* Main Content */}
      <main className="main-content">
        {/* Welcome Header */}
        <div className="welcome-header">
          <h1 className="welcome-title">Welcome to AI PDF Assistant</h1>
          <p className="welcome-subtitle">
            Extract insights and ask questions from your PDF documents instantly using LangChain4j RAG and Google Gemini.
          </p>
        </div>

        {/* Global Notifications */}
        {error && (
          <div className="alert-banner">
            <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
              <AlertCircle size={18} />
              <span>{error}</span>
            </div>
            <button onClick={() => setError(null)} style={{ background: 'none', border: 'none', cursor: 'pointer', color: '#991b1b' }}>
              <X size={16} />
            </button>
          </div>
        )}

        {success && (
          <div className="alert-banner" style={{ backgroundColor: '#f0fdf4', borderColor: '#bbf7d0', color: '#15803d' }}>
            <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
              <CheckCircle2 size={18} />
              <span>{success}</span>
            </div>
            <button onClick={() => setSuccess(null)} style={{ background: 'none', border: 'none', cursor: 'pointer', color: '#15803d' }}>
              <X size={16} />
            </button>
          </div>
        )}

        {/* Top Grid: Upload & Statistics */}
        <div className="dashboard-grid">
          {/* PDF Upload Card */}
          <div className="card">
            <div className="card-title">
              <Upload size={20} color="#22c55e" />
              <span>PDF Upload Card</span>
            </div>

            {!stats.pdfUploaded ? (
              <div 
                className={`dropzone ${dragActive ? 'active' : ''}`}
                onDragEnter={handleDrag}
                onDragLeave={handleDrag}
                onDragOver={handleDrag}
                onDrop={handleDrop}
                onClick={() => fileInputRef.current?.click()}
              >
                <input 
                  type="file" 
                  ref={fileInputRef} 
                  onChange={handleFileChange} 
                  accept="application/pdf" 
                  style={{ display: 'none' }} 
                />
                
                {uploading ? (
                  <>
                    <div className="spinner-dark"></div>
                    <span className="upload-text-main">Parsing text & generating vector embeddings...</span>
                    <span className="upload-text-sub">Please wait a moment</span>
                  </>
                ) : (
                  <>
                    <div className="upload-icon-wrapper">
                      <FileText size={26} />
                    </div>
                    <span className="upload-text-main">Click or Drag & Drop PDF file here</span>
                    <span className="upload-text-sub">Supports single PDF files (up to 15MB)</span>
                  </>
                )}
              </div>
            ) : (
              <div>
                <div className="uploaded-file-banner">
                  <div className="file-info">
                    <FileText size={28} color="#22c55e" />
                    <div>
                      <div className="file-name">{stats.fileName}</div>
                      <div className="file-meta">{stats.chunksCreated} Chunks Ingested in InMemoryEmbeddingStore</div>
                    </div>
                  </div>
                  <button className="btn-remove" onClick={handleReset} title="Remove PDF and reset session">
                    <X size={18} />
                  </button>
                </div>
                <div style={{ marginTop: '1rem', display: 'flex', justifyContent: 'flex-end' }}>
                  <button 
                    onClick={() => fileInputRef.current?.click()} 
                    style={{
                      display: 'flex',
                      alignItems: 'center',
                      gap: '6px',
                      background: 'none',
                      border: '1px solid var(--color-border)',
                      padding: '0.4rem 0.8rem',
                      borderRadius: 'var(--radius-md)',
                      fontSize: '0.85rem',
                      cursor: 'pointer',
                      color: 'var(--color-text-muted)'
                    }}
                  >
                    <RefreshCw size={14} /> Replace PDF
                  </button>
                  <input type="file" ref={fileInputRef} onChange={handleFileChange} accept="application/pdf" style={{ display: 'none' }} />
                </div>
              </div>
            )}
          </div>

          {/* Project Statistics Card */}
          <div className="card">
            <div className="card-title">
              <Layers size={20} color="#22c55e" />
              <span>Project Statistics</span>
            </div>

            <div className="stats-grid">
              <div className="stat-box">
                <div className="stat-icon">
                  <FileText size={20} />
                </div>
                <div className="stat-info">
                  <span className="stat-label">PDF Uploaded</span>
                  <div>
                    {stats.pdfUploaded ? (
                      <span className="status-badge yes">Yes</span>
                    ) : (
                      <span className="status-badge no">No</span>
                    )}
                  </div>
                </div>
              </div>

              <div className="stat-box">
                <div className="stat-icon">
                  <HelpCircle size={20} />
                </div>
                <div className="stat-info">
                  <span className="stat-label">Questions Asked</span>
                  <span className="stat-value">{stats.questionsAsked}</span>
                </div>
              </div>

              <div className="stat-box">
                <div className="stat-icon">
                  <Layers size={20} />
                </div>
                <div className="stat-info">
                  <span className="stat-label">Chunks Created</span>
                  <span className="stat-value">{stats.chunksCreated}</span>
                </div>
              </div>

              <div className="stat-box">
                <div className="stat-icon">
                  <Cpu size={20} />
                </div>
                <div className="stat-info">
                  <span className="stat-label">AI Model Used</span>
                  <span className="stat-value" style={{ fontSize: '0.95rem' }}>Gemini</span>
                </div>
              </div>
            </div>
          </div>
        </div>

        {/* Bottom Section: Recent Questions & Chat Interface */}
        <div className="dashboard-grid" style={{ gridTemplateColumns: '1fr 2fr' }}>
          {/* Recent Questions Section */}
          <div className="card">
            <div className="card-title">
              <MessageSquare size={20} color="#22c55e" />
              <span>Recent Questions</span>
            </div>

            {stats.recentQuestions && stats.recentQuestions.length > 0 ? (
              <div className="recent-questions-list">
                {stats.recentQuestions.map((q, idx) => (
                  <div 
                    key={idx} 
                    className="recent-item"
                    onClick={() => setQuestion(q)}
                    title="Click to ask this question again"
                  >
                    <span>{q}</span>
                    <Sparkles size={14} color="#22c55e" />
                  </div>
                ))}
              </div>
            ) : (
              <div style={{ color: 'var(--color-text-muted)', fontSize: '0.875rem', fontStyle: 'italic', textAlign: 'center', padding: '2rem 1rem' }}>
                No questions asked yet in this session. Ask a question below to see recent queries here!
              </div>
            )}
          </div>

          {/* Chat Section */}
          <div className="card chat-card">
            <div className="card-title">
              <Bot size={20} color="#22c55e" />
              <span>AI Chat Assistant</span>
            </div>

            {/* Message Area */}
            <div className="chat-messages">
              {messages.map((msg, index) => (
                <div key={index} className={`chat-bubble ${msg.sender}`}>
                  <div className="chat-header-info">
                    {msg.sender === 'user' ? (
                      <>
                        <User size={14} />
                        <span>You</span>
                      </>
                    ) : (
                      <>
                        <Bot size={14} />
                        <span>Gemini AI</span>
                      </>
                    )}
                    <span style={{ fontSize: '0.7rem', opacity: 0.8, marginLeft: 'auto' }}>
                      {msg.timestamp}
                    </span>
                  </div>

                  <div style={{ whiteSpace: 'pre-wrap' }}>
                    {msg.text}
                  </div>

                  {/* Retrieved Context Accordion for AI responses */}
                  {msg.retrievedContext && msg.retrievedContext.length > 0 && (
                    <div className="context-accordion">
                      <div className="context-toggle" onClick={() => toggleContext(index)}>
                        <span>{expandedContext[index] ? 'Hide Retrieved RAG Chunks' : `View ${msg.retrievedContext.length} Retrieved Context Chunks`}</span>
                        {expandedContext[index] ? <ChevronUp size={14} /> : <ChevronDown size={14} />}
                      </div>
                      
                      {expandedContext[index] && (
                        <div>
                          {msg.retrievedContext.map((snippet, sIdx) => (
                            <div key={sIdx} className="context-box">
                              <strong>Chunk #{sIdx + 1}:</strong> {snippet}
                            </div>
                          ))}
                        </div>
                      )}
                    </div>
                  )}
                </div>
              ))}

              {/* Loading animation when waiting for AI */}
              {asking && (
                <div className="chat-bubble ai">
                  <div className="chat-header-info">
                    <Bot size={14} />
                    <span>Gemini AI</span>
                  </div>
                  <div style={{ display: 'flex', alignItems: 'center', gap: '8px', color: 'var(--color-text-muted)' }}>
                    <div className="spinner-dark"></div>
                    <span>Retrieving context & generating answer...</span>
                  </div>
                </div>
              )}

              <div ref={messagesEndRef} />
            </div>

            {/* Input Form */}
            <form onSubmit={handleAsk} className="chat-input-form">
              <input
                type="text"
                className="chat-input"
                placeholder={stats.pdfUploaded ? "Ask a question related to the uploaded PDF..." : "Please upload a PDF file first..."}
                value={question}
                onChange={(e) => setQuestion(e.target.value)}
                disabled={!stats.pdfUploaded || asking}
              />
              <button 
                type="submit" 
                className="btn-send"
                disabled={!stats.pdfUploaded || !question.trim() || asking}
              >
                {asking ? (
                  <div className="spinner"></div>
                ) : (
                  <>
                    <span>Send</span>
                    <Send size={16} />
                  </>
                )}
              </button>
            </form>
          </div>
        </div>
      </main>
    </div>
  );
}
