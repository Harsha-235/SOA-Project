import api from "./api";

const login = async (email, password) => {
  const response = await api.post("/auth/login", {
    email,
    password,
  });

  return response.data;
};

const register = async (
  name,
  email,
  mobileNumber,
  password
) => {
  const response = await api.post("/auth/register", {
    name,
    email,
    mobileNumber,
    password,
  });

  return response.data;
};

const verifyEmailOtp = async (email, otp) => {
  const response = await api.post("/auth/verify-email-otp", {
    email,
    otp,
  });

  return response.data;
};

const verifyMobileOtp = async (email, otp) => {
  const response = await api.post("/auth/verify-mobile-otp", {
    email,
    otp,
  });

  return response.data;
};

const resendEmailOtp = async (email) => {
  const response = await api.post("/auth/resend-email-otp", {
    email,
  });

  return response.data;
};

const resendMobileOtp = async (email) => {
  const response = await api.post("/auth/resend-mobile-otp", {
    email,
  });

  return response.data;
};

export default {
  login,
  register,
  verifyEmailOtp,
  verifyMobileOtp,
  resendEmailOtp,
  resendMobileOtp,
};