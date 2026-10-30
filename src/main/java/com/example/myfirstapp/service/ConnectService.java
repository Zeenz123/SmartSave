package com.example.myfirstapp.service;

import com.example.myfirstapp.dao.ConnectionDao;
import com.example.myfirstapp.dao.GuardianDao;
import com.example.myfirstapp.dao.PaymentRequestDao;
import com.example.myfirstapp.dao.UserDao;
import com.example.myfirstapp.model.Guardian;
import com.example.myfirstapp.model.User;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class ConnectService {
    private final UserDao userDao;
    private final ConnectionDao connectionDao;
    private final GuardianDao guardianDao;
    private final PaymentRequestDao paymentRequestDao;
    private final WalletService walletService = new WalletService();

    public ConnectService() {
        this.userDao = new UserDao();
        this.connectionDao = new ConnectionDao();
        this.guardianDao = new GuardianDao();
        this.paymentRequestDao = new PaymentRequestDao();
    }

    public ConnectService(UserDao userDao, ConnectionDao connectionDao, GuardianDao guardianDao, PaymentRequestDao paymentRequestDao) {
        this.userDao = userDao;
        this.connectionDao = connectionDao;
        this.guardianDao = guardianDao;
        this.paymentRequestDao = paymentRequestDao;
    }

    public List<User> searchUsers(String query, int currentUserId) {
        if (query == null || query.isBlank()) {
            return userDao.findAllExcept(currentUserId);
        }
        return userDao.searchUsers(query.trim(), currentUserId);
    }

    public boolean connectUser(int currentUserId, int targetUserId, String relationshipType) {
        boolean connected = connectionDao.addConnection(currentUserId, targetUserId, relationshipType);
        if (connected && ("Parent".equalsIgnoreCase(relationshipType) || "Close Friend".equalsIgnoreCase(relationshipType))) {
            Optional<User> target = userDao.findById(targetUserId);
            target.ifPresent(u -> guardianDao.addGuardian(currentUserId, targetUserId, u.getFullName(), relationshipType));
        }
        return connected;
    }

    public List<User> getConnectedUsers(int currentUserId) {
        List<Integer> ids = connectionDao.getConnectedUserIds(currentUserId);
        List<User> list = new ArrayList<>();
        for (int id : ids) {
            userDao.findById(id).ifPresent(list::add);
        }
        return list;
    }

    public List<Guardian> getGuardians(int currentUserId) {
        return guardianDao.findByUserId(currentUserId);
    }

    public List<GuardianDao.WardRecord> getWardsWhoAddedUser(int guardianUserId, String guardianName, String guardianUsername) {
        return guardianDao.findWardsForGuardian(guardianUserId, guardianName, guardianUsername);
    }

    public void addGuardian(int currentUserId, String name, String relationship) {
        Integer matchedGuardianUserId = null;
        if (name != null && !name.isBlank()) {
            Optional<User> byIdentifier = userDao.findByPhoneOrEmailOrUsername(name.trim());
            if (byIdentifier.isPresent()) {
                matchedGuardianUserId = byIdentifier.get().getId();
            } else {
                List<User> all = userDao.findAllExcept(currentUserId);
                for (User u : all) {
                    if (u.getFullName().equalsIgnoreCase(name.trim()) || u.getUsername().equalsIgnoreCase(name.trim())) {
                        matchedGuardianUserId = u.getId();
                        break;
                    }
                }
            }
        }
        guardianDao.addGuardian(currentUserId, matchedGuardianUserId, name, relationship);
    }

    public boolean linkWardSample(int wardUserId, int guardianUserId, String guardianName, String relationship) {
        guardianDao.addGuardian(wardUserId, guardianUserId, guardianName, relationship);
        return true;
    }

    public void removeGuardian(int guardianId) {
        guardianDao.removeGuardian(guardianId);
    }

    /** A pending incoming payment request together with the requester's profile. */
    public static class IncomingRequest {
        public final PaymentRequestDao.Record record;
        public final User requester;

        public IncomingRequest(PaymentRequestDao.Record record, User requester) {
            this.record = record;
            this.requester = requester;
        }
    }

    public List<IncomingRequest> getIncomingRequests(int userId) {
        List<IncomingRequest> out = new ArrayList<>();
        for (PaymentRequestDao.Record r : paymentRequestDao.findPendingForTarget(userId)) {
            userDao.findById(r.requesterId).ifPresent(u -> out.add(new IncomingRequest(r, u)));
        }
        return out;
    }

    public List<PaymentRequestDao.Record> getOutgoingRequests(int userId) {
        return paymentRequestDao.findByRequesterId(userId);
    }

    /** Pays an incoming request from the payer's wallet to the requester (app-to-app). */
    public WalletService.TransferResult payRequest(int requestId, User payer) {
        PaymentRequestDao.Record rec = paymentRequestDao.findById(requestId).orElse(null);
        if (rec == null || rec.targetId == null || rec.targetId != payer.getId()) {
            return new WalletService.TransferResult(false, "This request is not addressed to you.", null);
        }
        if (!paymentRequestDao.transition(requestId, "PENDING", "PROCESSING")) {
            return new WalletService.TransferResult(false, "Request was already handled.", null);
        }
        User requester = userDao.findById(rec.requesterId).orElse(null);
        if (requester == null) {
            paymentRequestDao.transition(requestId, "PROCESSING", "PENDING");
            return new WalletService.TransferResult(false, "Requester no longer exists.", null);
        }
        WalletService.TransferResult result = walletService.transferFunds(payer.getId(), requester.getUsername(),
                rec.amount, "Paid request #" + requestId);
        paymentRequestDao.transition(requestId, "PROCESSING", result.isSuccess() ? "PAID" : "PENDING");
        return result;
    }

    public boolean declineRequest(int requestId, User user) {
        PaymentRequestDao.Record rec = paymentRequestDao.findById(requestId).orElse(null);
        return rec != null && rec.targetId != null && rec.targetId == user.getId()
                && paymentRequestDao.transition(requestId, "PENDING", "DECLINED");
    }

    public WalletService.TransferResult sendMoney(User sender, String recipientUsername, double amount) {
        return walletService.transferFunds(sender.getId(), recipientUsername, amount, "Quick pay via Connect");
    }

    public boolean sendPaymentRequest(int requesterId, String contact, double amount) {
        if (amount <= 0.0 || contact == null || contact.isBlank()) return false;
        Optional<User> target = userDao.findByPhoneOrEmailOrUsername(contact.trim());
        Integer targetId = target.map(User::getId).orElse(null);
        return paymentRequestDao.createRequest(requesterId, targetId, contact.trim(), amount);
    }
}
