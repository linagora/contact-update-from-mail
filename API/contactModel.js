var mongoose = require('mongoose');
// Setup schema
var contactSchema = mongoose.Schema({
    dest: {
        type: String,
        required: true
    },
    exp: {
        type: String,
        required: true
    },
    create_date: {
        type: Date,
        default: Date.now
    },
    phone: {type: String},
    job: {type: String}
}, { versionKey: false
});
// Export Contact model
var Contact = module.exports = mongoose.model('contact', contactSchema);
module.exports.get = function (callback) {
    Contact.find(callback);
}