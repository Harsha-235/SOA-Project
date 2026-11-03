import api from "./api";

const getUserFines = async (userId) => {
  const response = await api.get(`/fines/user/${userId}`);
  return response.data;
};

const submitPayment = async (fineId) => {
  const response = await api.put(`/fines/${fineId}/payment-submitted`);
  return response.data;
};

const getFineById = async (fineId) => {
  const response = await api.get(`/fines/${fineId}`);
  return response.data;
};

const approvePayment = async (fineId) => {
  const response = await api.put(`/fines/${fineId}/payment-approve`);
  return response.data;
};

const rejectPayment = async (fineId) => {
  const response = await api.put(`/fines/${fineId}/payment-reject`);
  return response.data;
};

export default {
  getUserFines,
  submitPayment,
  getFineById,
  approvePayment,
  rejectPayment,
};