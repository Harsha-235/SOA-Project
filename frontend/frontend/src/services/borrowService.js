import api from "./api";

const getUserBorrows = async (userId) => {
  const response = await api.get(`/borrows/user/${userId}`);
  return response.data;
};

const returnBorrow = async (borrowId) => {
  const response = await api.put(`/borrows/${borrowId}/return`);
  return response.data;
};

export default {
  getUserBorrows,
  returnBorrow,
};