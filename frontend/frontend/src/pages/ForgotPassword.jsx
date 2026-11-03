import { useState } from "react";
import { Mail, ArrowRight, LibraryBig } from "lucide-react";
import { Link, useNavigate } from "react-router-dom";
import api from "../services/api";

function ForgotPassword() {
  const navigate = useNavigate();

  const [email, setEmail] = useState("");
  const [loading, setLoading] = useState(false);

  const handleSubmit = async (e) => {
    e.preventDefault();

    try {
      setLoading(true);

      await api.post("/auth/forgot-password", {
        email,
      });

      alert("OTP has been sent to your email.");

      navigate("/reset-password", {
        state: { email },
      });
    } catch (error) {
      console.error("Forgot password failed:", error);

      const message =
        error.response?.data?.message ||
        error.response?.data ||
        "Unable to send password reset OTP.";

      alert(message);
    } finally {
      setLoading(false);
    }
  };

  return (
    <main className="login-page">
      <section className="login-card">

        <div className="login-header">
          <div className="login-logo">
            <LibraryBig size={25} strokeWidth={1.7} />
          </div>

          <div className="login-brand">
            <h1>KLU</h1>
            <span>Digital E-Library</span>
          </div>
        </div>

        <div className="login-heading">
          <span className="section-label">PASSWORD RECOVERY</span>

          <h2>Forgot Password?</h2>

          <p>
            Enter your registered email address and we will send you
            an OTP to reset your password.
          </p>
        </div>

        <form onSubmit={handleSubmit} className="login-form">

          <div className="form-group">
            <label htmlFor="email">Email address</label>

            <div className="input-wrapper">
              <Mail size={18} />

              <input
                id="email"
                type="email"
                placeholder="you@example.com"
                value={email}
                onChange={(e) => setEmail(e.target.value)}
                required
              />
            </div>
          </div>

          <button
            type="submit"
            className="login-button"
            disabled={loading}
          >
            <span>
              {loading ? "Sending..." : "Send OTP"}
            </span>

            {!loading && <ArrowRight size={18} />}
          </button>

        </form>

        <Link to="/login" className="register-button">
          Back to Login
        </Link>

        <p className="security-note">
          Secure password recovery · Your information remains protected
        </p>

      </section>
    </main>
  );
}

export default ForgotPassword;