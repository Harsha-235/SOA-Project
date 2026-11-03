import api from "./api";

const getUserById = async (userId) => {
  const response = await api.get(`/auth/users/${userId}`);
  return response.data;
};

export default {
  getUserById,
};