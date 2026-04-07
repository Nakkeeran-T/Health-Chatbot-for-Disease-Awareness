import { useState, useEffect } from 'react';
import { vaccineAPI } from '../services/api';
import { useLanguage } from '../context/language';
import './InfoPage.css';

const VaccinePage = () => {
  const [vaccines, setVaccines] = useState([]);
  const [loading, setLoading]   = useState(true);
  const { t, language } = useLanguage();

  useEffect(() => {
    vaccineAPI.getAll().then(res => setVaccines(res.data)).finally(() => setLoading(false));
  }, []);

  const getName = (v) => {
    if (language === 'hi' && v.vaccineNameHi) return v.vaccineNameHi;
    if (language === 'or' && v.vaccineNameOr) return v.vaccineNameOr;
    return v.vaccineName;
  };

  // Timeline ages don't change (medical standard) — only the surrounding UI changes language
  const timeline = [
    { age: 'At Birth',      vaccines: ['BCG', 'OPV-0', 'Hepatitis-B'],                             color: '#00d4aa' },
    { age: '6 Weeks',       vaccines: ['DPT-1', 'IPV-1', 'Hib-1', 'Rotavirus-1', 'Hepatitis-B'],  color: '#6c63ff' },
    { age: '10 Weeks',      vaccines: ['DPT-2', 'IPV-2', 'Hib-2', 'Rotavirus-2'],                 color: '#ffd93d' },
    { age: '14 Weeks',      vaccines: ['DPT-3', 'IPV-3', 'Hib-3', 'Rotavirus-3'],                 color: '#ff8c00' },
    { age: '9-12 Months',   vaccines: ['MR Vaccine', 'Typhoid (TCV)'],                             color: '#ff6b6b' },
    { age: '16-24 Months',  vaccines: ['DPT Booster', 'OPV Booster', 'MR-2'],                     color: '#00d4aa' },
  ];

  return (
    <div className="page info-page">
      <div className="container">
        <div className="page-header">
          <h1>💉 {t('vaccinesTitle')}</h1>
          <p>{t('vaccinesSub')}</p>
        </div>

        {/* NHM Banner */}
        <div className="nhm-banner">
          <span>🏛️</span>
          <div>
            <strong>{t('nhmBannerTitle')}</strong>
            <p>{t('nhmBannerDesc')}</p>
          </div>
        </div>

        {loading ? (
          <div className="loading-grid">
            {[...Array(6)].map((_, i) => <div key={i} className="skeleton-card"></div>)}
          </div>
        ) : (
          <div className="vaccine-table-wrapper">
            <table className="vaccine-table">
              <thead>
                <tr>
                  <th>{t('vaccineCol')}</th>
                  <th>{t('diseaseProtected')}</th>
                  <th>{t('targetAge')}</th>
                  <th>{t('doses')}</th>
                  <th>{t('route')}</th>
                  <th>{t('nhm')}</th>
                </tr>
              </thead>
              <tbody>
                {vaccines.map((v) => (
                  <tr key={v.id}>
                    <td>
                      <div className="vaccine-name">{getName(v)}</div>
                      {v.vaccineName !== getName(v) && <div className="vaccine-orig">{v.vaccineName}</div>}
                    </td>
                    <td><span className="badge badge-info">{v.disease}</span></td>
                    <td className="age-cell">{v.targetAge}</td>
                    <td className="doses-cell">{v.numberOfDoses}</td>
                    <td className="route-cell">{v.administrationRoute}</td>
                    <td>
                      {v.mandatoryUnderNHM
                        ? <span className="badge badge-success">✓ {t('free')}</span>
                        : <span className="badge">{t('available')}</span>}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}

        {/* Schedule timeline */}
        <h2 className="section-subtitle">{t('keyVaccineSchedule')}</h2>
        <div className="schedule-timeline">
          {timeline.map((s, i) => (
            <div key={i} className="timeline-item">
              <div className="timeline-dot" style={{ background: s.color }}></div>
              <div className="timeline-content">
                <div className="timeline-age" style={{ color: s.color }}>{s.age}</div>
                <div className="timeline-vaccines">
                  {s.vaccines.map((v) => <span key={v} className="vaccine-chip">{v}</span>)}
                </div>
              </div>
            </div>
          ))}
        </div>
      </div>
    </div>
  );
};

export default VaccinePage;
