var mongoose = require('mongoose');
// Setup schema
var exclusionSchema = mongoose.Schema({
    dest: {
        type: String,
        required: true
    },
    exp: {
        type: String,
        required: true
    },
    phones: [String],
    jobs: [String]
}, { versionKey: false
});
// Export Exclusion model
var Exclusion = module.exports = mongoose.model('exclusion', exclusionSchema);
module.exports.get = function (callback) {
    Exclusion.find(callback);
}