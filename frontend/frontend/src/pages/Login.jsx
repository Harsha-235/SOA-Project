import { useState } from "react";
import authService from "../services/authService";
import {
  Eye,
  EyeOff,
  LockKeyhole,
  Mail,
  ArrowRight,
  LibraryBig,
} from "lucide-react";
import { Link, useNavigate } from "react-router-dom";

function Login() {
    const navigate = useNavigate();
  const [showPassword, setShowPassword] = useState(false);
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");

const handleSubmit = async (e) => {
  e.preventDefault();

  try {
    const data = await authService.login(email, password);

    localStorage.setItem("archivalia_token", data.token);

    // Read role from JWT
    const payload = JSON.parse(
      atob(data.token.split(".")[1].replace(/-/g, "+").replace(/_/g, "/"))
    );

    const role = payload.role;

    if (role === "ADMIN") {
      navigate("/admin");
    } else {
      navigate("/dashboard");
    }
  } catch (error) {
    console.error("Login failed:", error);

    const message =
      error.response?.data?.message || "Invalid email or password";

    alert(message);
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
            <h1>ARCHIVALIA</h1>
            <span>Digital E-Library</span>
          </div>
        </div>

        <div className="login-heading">
          <span className="section-label">MEMBER ACCESS</span>

          <h2>Welcome</h2>

          <p>
            Sign in to access your library account and continue your
            learning journey.
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

          <div className="form-group">
            <div className="label-row">
              <label htmlFor="password">Password</label>

              <button
  type="button"
  className="forgot-button"
  onClick={() => navigate("/forgot-password")}
>
  Forgot password?
</button>
            </div>

            <div className="input-wrapper">
              <LockKeyhole size={18} />

              <input
                id="password"
                type={showPassword ? "text" : "password"}
                placeholder="Enter your password"
                value={password}
                onChange={(e) => setPassword(e.target.value)}
                required
              />

              <button
                type="button"
                className="password-toggle"
                onClick={() => setShowPassword(!showPassword)}
              >
                {showPassword ? (
                  <EyeOff size={18} />
                ) : (
                  <Eye size={18} />
                )}
              </button>
            </div>
          </div>

          <button type="submit" className="login-button">
            <span>Sign in</span>
            <ArrowRight size={18} />
          </button>
        </form>

        <div className="login-divider">
          <span>NEW TO ARCHIVALIA?</span>
        </div>

        <Link to="/register" className="register-button">
          Create your library account
        </Link>

        <p className="security-note">
          Secure authentication · Your information remains protected
        </p>
      </section>
    </main>
  );
}

export default Login;